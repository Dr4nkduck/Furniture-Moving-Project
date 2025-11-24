package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.CancellationRequest;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.CancellationRequestRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API cho phía Provider xử lý yêu cầu hủy đơn (giai đoạn 3)
 *
 * - GET  /api/provider/cancellations?status=requested
 * - POST /api/provider/cancellations/{id}/approve
 * - POST /api/provider/cancellations/{id}/reject
 */
@RestController
@RequestMapping("/api/provider/cancellations")
public class ProviderCancellationApiController {

    private final CancellationRequestRepository cancellationRequestRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final JdbcTemplate jdbc;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ProviderCancellationApiController(
            CancellationRequestRepository cancellationRequestRepository,
            ServiceRequestRepository serviceRequestRepository,
            JdbcTemplate jdbc
    ) {
        this.cancellationRequestRepository = cancellationRequestRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.jdbc = jdbc;
    }

    // ===== Helper: lấy username đang đăng nhập =====
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return auth.getName();
    }

    // ===== Helper: lấy user_id từ username =====
    private Integer getCurrentUserId() {
        String username = getCurrentUsername();
        try {
            return jdbc.queryForObject(
                    "SELECT user_id FROM users WHERE username = ?",
                    Integer.class,
                    username
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không tìm thấy tài khoản người dùng"
            );
        }
    }

    // ===== Helper: lấy provider_id từ username =====
    private Integer getCurrentProviderId() {
        String username = getCurrentUsername();
        try {
            return jdbc.queryForObject(
                    """
                    SELECT p.provider_id
                    FROM providers p
                    JOIN users u ON u.user_id = p.user_id
                    WHERE u.username = ?
                    """,
                    Integer.class,
                    username
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản hiện tại không phải nhà cung cấp"
            );
        }
    }

    // ===== Helper: đảm bảo request thuộc provider & đang ở trạng thái requested =====
    private CancellationRequest requireOwnedRequestedCancellation(Integer cancellationId, Integer providerId) {
        CancellationRequest cr = cancellationRequestRepository.findById(cancellationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (cr.getProviderId() == null || !cr.getProviderId().equals(providerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xử lý yêu cầu hủy này"
            );
        }

        if (!CancellationRequest.STATUS_REQUESTED.equalsIgnoreCase(
                Optional.ofNullable(cr.getStatus()).orElse("")
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Yêu cầu hủy này đã được xử lý (approved/rejected)."
            );
        }

        return cr;
    }

    // =====================================================================
    //  GET LIST: Provider xem danh sách yêu cầu hủy của mình
    // =====================================================================
    @GetMapping
    public List<CancellationSummaryDTO> getMyCancellations(
            @RequestParam(value = "status", required = false) String status
    ) {
        Integer providerId = getCurrentProviderId();

        List<CancellationRequest> list;
        if (status != null && !status.isBlank()) {
            list = cancellationRequestRepository
                    .findByProviderIdAndStatusOrderByCreatedAtDesc(providerId, status);
        } else {
            list = cancellationRequestRepository
                    .findByProviderIdOrderByCreatedAtDesc(providerId);
        }

        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        // Lấy kèm thông tin service_request để hiển thị gọn bên FE
        Set<Integer> requestIds = list.stream()
                .map(CancellationRequest::getServiceRequestId)
                .collect(Collectors.toSet());

        Map<Integer, ServiceRequest> srMap = serviceRequestRepository.findAllById(requestIds)
                .stream()
                .collect(Collectors.toMap(ServiceRequest::getRequestId, sr -> sr));

        List<CancellationSummaryDTO> dtos = new ArrayList<>();
        for (CancellationRequest cr : list) {
            ServiceRequest sr = srMap.get(cr.getServiceRequestId());
            dtos.add(CancellationSummaryDTO.from(cr, sr, dtf, dateOnly));
        }

        return dtos;
    }

    // =====================================================================
    //  APPROVE: Provider chấp nhận yêu cầu hủy
    // =====================================================================
    @PostMapping("/{cancellationId}/approve")
    public Map<String, Object> approveCancellation(
            @PathVariable Integer cancellationId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        Integer providerId = getCurrentProviderId();
        Integer currentUserId = getCurrentUserId();

        CancellationRequest cr = requireOwnedRequestedCancellation(cancellationId, providerId);

        ServiceRequest sr = serviceRequestRepository.findById(cr.getServiceRequestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn dịch vụ"));

        String note = body != null ? body.getOrDefault("note", "") : "";
        note = note == null ? "" : note.trim();

        // Cập nhật cancellation_requests
        cr.setStatus(CancellationRequest.STATUS_APPROVED);
        cr.setDecidedAt(LocalDateTime.now());
        cr.setDecidedBy(currentUserId);
        cr.setDecisionNote(note);
        cancellationRequestRepository.save(cr);

        // Cập nhật service_requests: đổi trạng thái sang cancelled
        String st = Optional.ofNullable(sr.getStatus()).orElse("").trim().toLowerCase();
        if (!"cancelled".equals(st)) {
            sr.setStatus("cancelled");
        }
        if (sr.getCancelReason() == null || sr.getCancelReason().isBlank()) {
            sr.setCancelReason(cr.getReason());
        }
        if (sr.getCancelledAt() == null) {
            sr.setCancelledAt(LocalDateTime.now());
        }
        serviceRequestRepository.save(sr);

        // (Tuỳ chọn) Hủy các payment đang PENDING
        jdbc.update(
                """
                UPDATE payments
                SET status = 'CANCELLED'
                WHERE service_request_id = ? AND status = 'PENDING'
                """,
                sr.getRequestId()
        );

        return Map.of(
                "success", true,
                "message", "Đã chấp nhận yêu cầu hủy đơn.",
                "cancellationStatus", cr.getStatus(),
                "serviceRequestStatus", sr.getStatus()
        );
    }

    // =====================================================================
    //  REJECT: Provider từ chối yêu cầu hủy
    // =====================================================================
    @PostMapping("/{cancellationId}/reject")
    public Map<String, Object> rejectCancellation(
            @PathVariable Integer cancellationId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        Integer providerId = getCurrentProviderId();
        Integer currentUserId = getCurrentUserId();

        CancellationRequest cr = requireOwnedRequestedCancellation(cancellationId, providerId);

        String note = body != null ? body.getOrDefault("note", "") : "";
        note = note == null ? "" : note.trim();

        if (note.isEmpty()) {
            note = "Provider từ chối yêu cầu hủy.";
        }

        cr.setStatus(CancellationRequest.STATUS_REJECTED);
        cr.setDecidedAt(LocalDateTime.now());
        cr.setDecidedBy(currentUserId);
        cr.setDecisionNote(note);
        cancellationRequestRepository.save(cr);

        return Map.of(
                "success", true,
                "message", "Đã từ chối yêu cầu hủy.",
                "cancellationStatus", cr.getStatus()
        );
    }

    // =====================================================================
    //  DTO tóm tắt gửi cho FE
    // =====================================================================
    public static class CancellationSummaryDTO {
        public Integer cancellationId;
        public Integer serviceRequestId;

        public String status;         // requested / approved / rejected
        public String reason;
        public String decisionNote;

        public String createdAt;      // dd/MM/yyyy HH:mm
        public String decidedAt;      // dd/MM/yyyy HH:mm (optional)

        // Một ít info từ service_requests (cho provider dễ nhìn)
        public String serviceStatus;
        public String paymentStatus;
        public String paymentType;
        public String totalCostFormatted;
        public String depositFormatted;
        public String preferredDate;

        public static CancellationSummaryDTO from(
                CancellationRequest cr,
                ServiceRequest sr,
                DateTimeFormatter dtf,
                DateTimeFormatter dateOnly
        ) {
            CancellationSummaryDTO dto = new CancellationSummaryDTO();
            dto.cancellationId = cr.getCancellationId();
            dto.serviceRequestId = cr.getServiceRequestId();
            dto.status = cr.getStatus();
            dto.reason = cr.getReason();
            dto.decisionNote = cr.getDecisionNote();

            if (cr.getCreatedAt() != null) {
                dto.createdAt = dtf.format(cr.getCreatedAt());
            }
            if (cr.getDecidedAt() != null) {
                dto.decidedAt = dtf.format(cr.getDecidedAt());
            }

            if (sr != null) {
                dto.serviceStatus = sr.getStatus();
                dto.paymentStatus = sr.getPaymentStatus();
                dto.paymentType = sr.getPaymentType();

                if (sr.getTotalCost() != null) {
                    dto.totalCostFormatted = String.format("%,.0f đ", sr.getTotalCost());
                }
                if (sr.getDepositAmount() != null) {
                    dto.depositFormatted = String.format("%,.0f đ", sr.getDepositAmount());
                }
                if (sr.getPreferredDate() != null) {
                    dto.preferredDate = dateOnly.format(sr.getPreferredDate());
                }
            }

            return dto;
        }
    }
}
