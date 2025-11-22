package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.ProviderOrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderItemDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderSummaryDTO;
import SWP301.Furniture_Moving_Project.model.CancellationRequest;
import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.CancellationRequestRepository;
import SWP301.Furniture_Moving_Project.repository.ContractRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderDetailProjection;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderItemProjection;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderSummaryProjection;
import SWP301.Furniture_Moving_Project.service.ProviderOrderService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProviderOrderServiceImpl implements ProviderOrderService {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ServiceRequestRepository srRepo;
    private final ContractRepository contractRepo;
    private final CancellationRequestRepository cancellationRequestRepository;

    public ProviderOrderServiceImpl(ServiceRequestRepository srRepo,
                                    ContractRepository contractRepo,
                                    CancellationRequestRepository cancellationRequestRepository) {
        this.srRepo = srRepo;
        this.contractRepo = contractRepo;
        this.cancellationRequestRepository = cancellationRequestRepository;
    }

    @Override
    public List<ProviderOrderSummaryDTO> listOrders(Integer providerId, String status, String q) {
        String s = StringUtils.hasText(status) ? status : null;
        String query = StringUtils.hasText(q) ? q.trim() : null;

        List<ProviderOrderSummaryProjection> rows = srRepo.findProviderOrders(providerId, s, query);
        return rows.stream().map(r -> new ProviderOrderSummaryDTO(
                r.getRequestId(),
                r.getStatus(),
                r.getRequestDate(),
                r.getPreferredDate(),
                (r.getCustomerFirstName() == null && r.getCustomerLastName() == null)
                        ? "N/A"
                        : (r.getCustomerFirstName() + " " + r.getCustomerLastName()).trim(),
                join(r.getPickupStreet(), r.getPickupCity()),
                join(r.getDeliveryStreet(), r.getDeliveryCity()),
                r.getTotalCost()
        )).collect(Collectors.toList());
    }

    @Override
    public ProviderOrderDetailDTO getOrderDetail(Integer providerId, Integer requestId) {
        // ✅ 1. Lấy projection chi tiết để đảm bảo đơn thuộc provider này
        ProviderOrderDetailProjection p = srRepo.findOrderDetail(providerId, requestId);
        if (p == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng hoặc đơn không thuộc về nhà cung cấp này.");
        }

        ProviderOrderDetailDTO dto = new ProviderOrderDetailDTO();
        dto.setRequestId(p.getRequestId());
        dto.setStatus(p.getStatus());
        dto.setRequestDate(p.getRequestDate());
        dto.setPreferredDate(p.getPreferredDate());
        dto.setTotalCostEstimate(p.getTotalCost());

        dto.setCustomerName((p.getCustomerFirstName() + " " + p.getCustomerLastName()).trim());
        dto.setCustomerPhone(p.getCustomerPhone());
        dto.setCustomerEmail(p.getCustomerEmail());

        dto.setPickupFull(joinFull(
                p.getPickupStreet(), p.getPickupCity(), p.getPickupState(), p.getPickupZip()));
        dto.setDeliveryFull(joinFull(
                p.getDeliveryStreet(), p.getDeliveryCity(), p.getDeliveryState(), p.getDeliveryZip()));

        // ✅ 2. Lấy danh sách item
        List<ProviderOrderItemProjection> items = srRepo.findOrderItems(requestId);
        dto.setItems(items.stream()
                .map(i -> new ProviderOrderItemDTO(
                        i.getItemId(),
                        i.getItemType(),
                        i.getSize(),
                        i.getQuantity() == null ? 0 : i.getQuantity(),
                        Boolean.TRUE.equals(i.getIsFragile()))
                )
                .collect(Collectors.toList()));

        // ✅ 3. Lấy thêm thông tin từ entity ServiceRequest (payment + cancelReason)
        ServiceRequest sr = srRepo.findById(requestId)
                .orElse(null);
        if (sr != null) {
            dto.setPaymentStatus(sr.getPaymentStatus());
            dto.setPaymentType(sr.getPaymentType());
            dto.setCancelReason(sr.getCancelReason());
        }

        // ✅ 4. Lấy yêu cầu hủy mới nhất (nếu có) cho đơn này của provider này
        if (sr != null && sr.getProviderId() != null) {
            Optional<CancellationRequest> optCr =
                    cancellationRequestRepository
                            .findTopByServiceRequestIdAndProviderIdOrderByCreatedAtDesc(
                                    sr.getRequestId(), sr.getProviderId());

            if (optCr.isPresent()) {
                CancellationRequest cr = optCr.get();
                dto.setCancellationId(cr.getCancellationId());
                dto.setCancellationStatus(cr.getStatus());
                dto.setCancellationReason(cr.getReason());
                dto.setCancellationDecisionNote(cr.getDecisionNote());
            }
        }

        return dto;
    }

    @Override
    public void updateOrderStatus(Integer providerId, Integer requestId, String newStatus, String cancelReason) {
        if (!StringUtils.hasText(newStatus)) {
            throw new IllegalArgumentException("Thiếu trạng thái cần cập nhật.");
        }

        String raw = newStatus.trim().toLowerCase();

        // Lấy request trước để biết trạng thái hiện tại + check quyền
        ServiceRequest request = srRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        if (request.getProviderId() == null || !request.getProviderId().equals(providerId)) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng hoặc đơn không thuộc về nhà cung cấp này.");
        }

        String current = request.getStatus() == null
                ? "pending"
                : request.getStatus().toLowerCase();

        String ns = raw;

        // Handle acknowledgment: provider bấm "accepted" -> acknowledge contract + chuyển sang ready_to_pay
        if ("accepted".equals(raw)) {
            // Chỉ cho accept khi đơn đang pending (tránh accept lại đơn đã đi xa hơn)
            if (!"pending".equals(current)) {
                throw new IllegalStateException("Chỉ có thể chấp nhận những đơn đang ở trạng thái \"Đang chờ xử lý\".");
            }

            if (request.getContractId() != null) {
                Contract contract = contractRepo.findById(request.getContractId())
                        .orElse(null);
                if (contract != null && "signed".equals(contract.getStatus())) {
                    contract.setStatus("acknowledged");
                    contract.setAcknowledgedAt(OffsetDateTime.now());
                    contractRepo.save(contract);
                }
            }
            ns = "ready_to_pay";
        }

        // Validate trạng thái đích (basic)
        switch (ns) {
            case "pending", "ready_to_pay", "declined", "in_progress", "completed", "cancelled" -> {
            }
            default -> throw new IllegalArgumentException("Trạng thái không được hỗ trợ: " + ns);
        }

        // Áp dụng luật transition (không cho completed -> ready_to_pay, v.v.)
        if (!canTransition(current, ns)) {
            String humanCurrent = toDisplayStatus(current);
            String humanTarget  = toDisplayStatus(ns);

            String msg;
            if (isTerminal(current)) {
                msg = "Đơn hiện đang ở trạng thái \"" + humanCurrent + "\" và đã được xem là kết thúc, "
                        + "nên không thể cập nhật thêm.";
            } else {
                msg = "Không thể chuyển trạng thái từ \"" + humanCurrent + "\" sang \"" + humanTarget + "\". "
                        + "Vui lòng kiểm tra lại quy trình xử lý đơn.";
            }
            throw new IllegalStateException(msg);
        }

        int updated = srRepo.providerUpdateStatus(
                providerId,
                requestId,
                ns,
                StringUtils.hasText(cancelReason) && "cancelled".equals(ns) ? cancelReason : null
        );
        if (updated == 0) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng hoặc đơn không thuộc về nhà cung cấp này.");
        }
    }

    /**
     * Provider bấm nút "Xác nhận đã thanh toán" sau khi tự kiểm tra sao kê.
     * Chỉ cho phép xác nhận nếu:
     *  - Đơn thuộc về provider này
     *  - Trạng thái hiện tại đang "ready_to_pay"
     * Sau đó set:
     *  - status          = "paid"
     *  - payment_status  = "PAID"
     *  - paid_at         = thời điểm hiện tại (VN)
     */
    @Override
    public void confirmPayment(Integer providerId, Integer requestId) {
        ServiceRequest sr = srRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        // Kiểm tra quyền sở hữu
        if (sr.getProviderId() == null || !sr.getProviderId().equals(providerId)) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng hoặc đơn không thuộc về nhà cung cấp này.");
        }

        String current = sr.getStatus() == null ? "" : sr.getStatus().toLowerCase();
        // Chỉ cho xác nhận khi đang chờ thanh toán
        if (!current.equals("ready_to_pay")) {
            String humanCurrent = toDisplayStatus(current);
            throw new IllegalStateException(
                    "Chỉ có thể xác nhận thanh toán cho đơn đang ở trạng thái \"Chờ khách thanh toán\". "
                            + "Trạng thái hiện tại: \"" + humanCurrent + "\"."
            );
        }

        // 🔥 Đánh dấu đã thanh toán: set đủ 3 field
        sr.setStatus("paid");
        sr.setPaymentStatus("PAID");
        if (sr.getPaidAt() == null) {
            sr.setPaidAt(LocalDateTime.now(ZONE_VN));
        }

        srRepo.save(sr);
    }

    // ===== helpers =====

    private static String join(String a, String b) {
        if (!StringUtils.hasText(a)) return StringUtils.hasText(b) ? b : "";
        return StringUtils.hasText(b) ? a + ", " + b : a;
    }

    private static String joinFull(String street, String city, String state, String zip) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(street)) sb.append(street);
        if (StringUtils.hasText(city)) sb.append(sb.length() > 0 ? ", " : "").append(city);
        if (StringUtils.hasText(state)) sb.append(sb.length() > 0 ? ", " : "").append(state);
        if (StringUtils.hasText(zip)) sb.append(" ").append(zip);
        return sb.toString();
    }

    /**
     * Luật chuyển trạng thái provider-order:
     *  - pending       -> ready_to_pay / declined / cancelled
     *  - ready_to_pay  -> in_progress / cancelled   (thanh toán xong thì in_progress hoặc hủy)
     *  - paid          -> in_progress / cancelled   (sau confirmPayment)
     *  - in_progress   -> completed / cancelled
     *  - completed/declined/cancelled -> KHÔNG cho đi đâu nữa (trừ gọi lại cùng status = idempotent)
     */
    private static boolean canTransition(String from, String to) {
        if (from == null || to == null) return false;
        from = from.toLowerCase();
        to = to.toLowerCase();

        // Gọi lại cùng trạng thái thì cho qua (idempotent), ví dụ: cancelled -> cancelled
        if (from.equals(to)) {
            return true;
        }

        return switch (from) {
            case "pending" ->
                    "ready_to_pay".equals(to)
                            || "declined".equals(to)
                            || "cancelled".equals(to);
            case "ready_to_pay", "paid" ->
                    "in_progress".equals(to)
                            || "cancelled".equals(to);
            case "in_progress" ->
                    "completed".equals(to)
                            || "cancelled".equals(to);
            case "completed", "cancelled", "declined" ->
                    false; // ✅ đã kết thúc, KHÔNG cho đổi nữa
            default -> false;
        };
    }

    private static boolean isTerminal(String s) {
        if (s == null) return false;
        s = s.toLowerCase();
        return "completed".equals(s) || "cancelled".equals(s) || "declined".equals(s);
    }

    private static String toDisplayStatus(String s) {
        if (s == null) return "Không xác định";
        return switch (s.toLowerCase()) {
            case "pending"      -> "Đang chờ xử lý";
            case "ready_to_pay" -> "Chờ khách thanh toán";
            case "paid"         -> "Đã thanh toán";
            case "in_progress"  -> "Đang thực hiện";
            case "completed"    -> "Hoàn thành";
            case "cancelled"    -> "Đã hủy";
            case "declined"     -> "Đã từ chối";
            default             -> s;
        };
    }
}
