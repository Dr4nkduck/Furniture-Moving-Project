package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.ProviderOrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderItemDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderSummaryDTO;
import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ContractRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderDetailProjection;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderItemProjection;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderSummaryProjection;
import SWP301.Furniture_Moving_Project.service.ProviderOrderService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProviderOrderServiceImpl implements ProviderOrderService {

    private final ServiceRequestRepository srRepo;
    private final ContractRepository contractRepo;

    public ProviderOrderServiceImpl(ServiceRequestRepository srRepo, ContractRepository contractRepo) {
        this.srRepo = srRepo;
        this.contractRepo = contractRepo;
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
                        ? "N/A" : (r.getCustomerFirstName() + " " + r.getCustomerLastName()).trim(),
                join(r.getPickupStreet(), r.getPickupCity()),
                join(r.getDeliveryStreet(), r.getDeliveryCity()),
                r.getTotalCost()
        )).collect(Collectors.toList());
    }

    @Override
    public ProviderOrderDetailDTO getOrderDetail(Integer providerId, Integer requestId) {
        ProviderOrderDetailProjection p = srRepo.findOrderDetail(providerId, requestId);
        if (p == null) throw new IllegalArgumentException("Order not found or not owned by provider");

        ProviderOrderDetailDTO dto = new ProviderOrderDetailDTO();
        dto.setRequestId(p.getRequestId());
        dto.setStatus(p.getStatus());
        dto.setRequestDate(p.getRequestDate());
        dto.setPreferredDate(p.getPreferredDate());
        dto.setTotalCostEstimate(p.getTotalCost());

        dto.setCustomerName((p.getCustomerFirstName() + " " + p.getCustomerLastName()).trim());
        dto.setCustomerPhone(p.getCustomerPhone());
        dto.setCustomerEmail(p.getCustomerEmail());

        dto.setPickupFull(joinFull(p.getPickupStreet(), p.getPickupCity(), p.getPickupState(), p.getPickupZip()));
        dto.setDeliveryFull(joinFull(p.getDeliveryStreet(), p.getDeliveryCity(), p.getDeliveryState(), p.getDeliveryZip()));

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
        return dto;
    }

    @Override
    public void updateOrderStatus(Integer providerId, Integer requestId, String newStatus, String cancelReason) {
        if (!StringUtils.hasText(newStatus)) {
            throw new IllegalArgumentException("Missing status");
        }
        String ns = newStatus.toLowerCase();

        // Get the request to check contract
        ServiceRequest request = srRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Handle acknowledgment: when provider accepts, acknowledge contract and set request to ready_to_pay
        if ("accepted".equals(ns)) {
            // Update contract to acknowledged if exists
            if (request.getContractId() != null) {
                Contract contract = contractRepo.findById(request.getContractId())
                        .orElse(null);
                if (contract != null && "signed".equals(contract.getStatus())) {
                    contract.setStatus("acknowledged");
                    contract.setAcknowledgedAt(OffsetDateTime.now());
                    contractRepo.save(contract);
                }
            }
            // Set request status to ready_to_pay instead of accepted
            ns = "ready_to_pay";
        }

        // transition rules (map theo yêu cầu PV-004)
        // pending -> ready_to_pay (via accepted)/declined/cancelled
        // ready_to_pay -> in_progress/cancelled (after payment)
        // in_progress -> completed/cancelled
        // completed/declined/cancelled -> only allow cancelled (idempotent) else reject
        switch (ns) {
            case "pending", "ready_to_pay", "declined", "in_progress", "completed", "cancelled" -> {}
            default -> throw new IllegalArgumentException("Unsupported status: " + ns);
        }

        int updated = srRepo.providerUpdateStatus(
                providerId,
                requestId,
                ns,
                StringUtils.hasText(cancelReason) && "cancelled".equals(ns) ? cancelReason : null
        );
        if (updated == 0) throw new IllegalArgumentException("Order not found or not owned by provider");
    }

    /**
     * Provider bấm nút "Xác nhận đã thanh toán" sau khi tự kiểm tra sao kê.
     * Luồng: chỉ cho phép xác nhận nếu:
     *  - Đơn thuộc về provider này
     *  - Trạng thái hiện tại đang "ready_to_pay" (hoặc "pending" tuỳ bạn)
     * Sau đó set status = "paid".
     */
    public void confirmPayment(Integer providerId, Integer requestId) {
        ServiceRequest sr = srRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Kiểm tra quyền sở hữu
        if (sr.getProviderId() == null || !sr.getProviderId().equals(providerId)) {
            throw new IllegalArgumentException("Order not owned by provider");
        }

        String current = sr.getStatus() == null ? "" : sr.getStatus().toLowerCase();
        // Chỉ cho xác nhận khi đang chờ thanh toán
        if (!current.equals("ready_to_pay")) {
            throw new IllegalStateException("Order is not in ready_to_pay state, cannot confirm payment.");
        }

        // Đánh dấu đã thanh toán
        sr.setStatus("paid");
        // Nếu sau này bạn có thêm trường paidAt thì có thể set ở đây:
        // sr.setPaidAt(LocalDateTime.now());

        srRepo.save(sr);
    }

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
}
