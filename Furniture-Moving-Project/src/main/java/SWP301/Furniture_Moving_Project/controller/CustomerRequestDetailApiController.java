package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.RatingRequestDTO;
import SWP301.Furniture_Moving_Project.dto.RatingResponseDTO;
import SWP301.Furniture_Moving_Project.model.CancellationRequest;
import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.model.Review;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.CancellationRequestRepository;
import SWP301.Furniture_Moving_Project.repository.ContractRepository;
import SWP301.Furniture_Moving_Project.repository.ReviewRepository;
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
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer/request")
public class CustomerRequestDetailApiController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ContractRepository contractRepository;
    private final JdbcTemplate jdbc;
    private final ReviewRepository reviewRepository;
    private final CancellationRequestRepository cancellationRequestRepository;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CustomerRequestDetailApiController(
            ServiceRequestRepository serviceRequestRepository,
            ContractRepository contractRepository,
            JdbcTemplate jdbc,
            ReviewRepository reviewRepository,
            CancellationRequestRepository cancellationRequestRepository
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.contractRepository = contractRepository;
        this.jdbc = jdbc;
        this.reviewRepository = reviewRepository;
        this.cancellationRequestRepository = cancellationRequestRepository;
    }

    /** Lấy customer_id của người đang đăng nhập dựa trên username */
    private Integer getCurrentCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String username = auth.getName();

        try {
            return jdbc.queryForObject("""
                SELECT c.customer_id
                FROM customers c
                JOIN users u ON u.user_id = c.user_id
                WHERE u.username = ?
            """, Integer.class, username);
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    /** Thông tin đơn dùng riêng cho rating / kiểm tra owner */
    private static class RequestOwnerInfo {
        Integer requestId;
        Integer customerId;
        Integer providerId;
        String status;
    }

    /** Đảm bảo request thuộc về customer đang đăng nhập, nếu không thì 403/404 */
    private RequestOwnerInfo requireCustomerOwnsRequest(Integer requestId) {
        Integer currentCustomerId = getCurrentCustomerId();

        String sql = """
            SELECT request_id, customer_id, provider_id, status
            FROM service_requests
            WHERE request_id = ?
            """;

        RequestOwnerInfo info;
        try {
            info = jdbc.queryForObject(sql,
                    (rs, rowNum) -> {
                        RequestOwnerInfo i = new RequestOwnerInfo();
                        i.requestId = rs.getInt("request_id");
                        i.customerId = rs.getInt("customer_id");
                        int provider = rs.getInt("provider_id");
                        i.providerId = rs.wasNull() ? null : provider;
                        i.status = rs.getString("status");
                        return i;
                    },
                    requestId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (!info.customerId.equals(currentCustomerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return info;
    }

    // ================== API DETAIL ĐƠN ==================

    @GetMapping("/{requestId}")
    public DetailDTO getRequestDetail(@PathVariable Integer requestId) {
        Integer customerId = getCurrentCustomerId();

        Optional<ServiceRequest> reqOpt = serviceRequestRepository.findById(requestId);
        if (reqOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        ServiceRequest r = reqOpt.get();
        if (!r.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Contract c = null;
        if (r.getContractId() != null) {
            c = contractRepository.findById(r.getContractId()).orElse(null);
        }

        return DetailDTO.from(r, c, dtf);
    }

    // ================== API CANCEL (HỦY TRỰC TIẾP – GIAI ĐOẠN 1) ==================

    @PostMapping("/{requestId}/cancel")
    public Map<String, Object> cancelRequest(
            @PathVariable Integer requestId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        // Đảm bảo đây là đơn của customer đang đăng nhập
        RequestOwnerInfo owner = requireCustomerOwnsRequest(requestId);

        // Lấy entity đầy đủ để dùng helper isCancellableByCustomer()
        ServiceRequest req = serviceRequestRepository.findById(owner.requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Rule hủy nằm trong entity:
        // - KHÔNG cho hủy nếu status = PAID / IN_PROGRESS / COMPLETED / CANCELLED
        // - KHÔNG cho hủy nếu payment_status = PAID
        if (!req.isCancellableByCustomer()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn đã được xử lý, bạn không thể hủy trực tiếp."
            );
        }

        String reason = (body != null) ? body.getOrDefault("reason", "") : "";

        // Đánh dấu trạng thái huỷ trên service_requests
        req.setStatus("cancelled");
        req.setCancelReason(reason);
        req.setCancelledAt(LocalDateTime.now());
        serviceRequestRepository.save(req);

        // (Tuỳ chọn) Nếu muốn, có thể đánh dấu các payment PENDING là CANCELLED
        jdbc.update("""
                UPDATE payments
                SET status = 'CANCELLED'
                WHERE service_request_id = ? AND status = 'PENDING'
                """, owner.requestId);

        return Map.of(
                "success", true,
                "message", "Hủy đơn thành công.",
                "newStatus", req.getStatus()
        );
    }

    // ================== API CANCEL REQUEST (GỬI YÊU CẦU HỦY – GIAI ĐOẠN 2) ==================

    @PostMapping("/{requestId}/cancel-request")
    public Map<String, Object> requestCancellation(
            @PathVariable Integer requestId,
            @RequestBody Map<String, String> body
    ) {
        RequestOwnerInfo owner = requireCustomerOwnsRequest(requestId);

        ServiceRequest req = serviceRequestRepository.findById(owner.requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Dùng rule trong entity để quyết định có được phép gửi yêu cầu hủy không
        if (!req.isCancellationRequestAllowedByCustomer()) {
            String st = req.getStatus() == null ? "" : req.getStatus().toLowerCase();
            if ("in_progress".equals(st) || "completed".equals(st) || "cancelled".equals(st)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đơn đã được thực hiện, bạn không thể gửi yêu cầu hủy."
                );
            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chỉ đơn đã thanh toán và chưa thực hiện mới được gửi yêu cầu hủy."
                );
            }
        }

        if (owner.providerId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn chưa được gán nhà cung cấp, không thể gửi yêu cầu hủy."
            );
        }

        // Kiểm tra đã có yêu cầu hủy đang chờ chưa
        boolean hasPending =
                cancellationRequestRepository.existsByServiceRequestIdAndStatus(
                        owner.requestId,
                        CancellationRequest.STATUS_REQUESTED
                );
        if (hasPending) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bạn đã gửi yêu cầu hủy và đang chờ đơn vị vận chuyển xử lý."
            );
        }

        String rawReason = body != null ? body.getOrDefault("reason", "") : "";
        String reason = rawReason == null ? "" : rawReason.trim();

        if (reason.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lý do hủy không được để trống."
            );
        }

        CancellationRequest cr = new CancellationRequest();
        cr.setServiceRequestId(owner.requestId);
        cr.setCustomerId(owner.customerId);
        cr.setProviderId(owner.providerId);
        cr.setReason(reason);
        // status, createdAt sẽ được set trong @PrePersist (mặc định REQUESTED)

        CancellationRequest saved = cancellationRequestRepository.save(cr);

        return Map.of(
                "success", true,
                "message", "Đã gửi yêu cầu hủy. Đơn vị vận chuyển sẽ xem xét và phản hồi.",
                "cancellationId", saved.getCancellationId()
        );
    }

    // ================== API RATING ==================

    @GetMapping("/{requestId}/rating")
    public RatingResponseDTO getRating(@PathVariable Integer requestId) {
        RequestOwnerInfo owner = requireCustomerOwnsRequest(requestId);

        Review review = reviewRepository.findByRequestId(owner.requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        RatingResponseDTO dto = new RatingResponseDTO();
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        if (review.getCreatedAt() != null) {
            dto.setCreatedAt(dtf.format(review.getCreatedAt()));
        }
        return dto;
    }

    @PutMapping("/{requestId}/rating")
    public RatingResponseDTO saveRating(@PathVariable Integer requestId,
                                        @RequestBody RatingRequestDTO body) {

        RequestOwnerInfo owner = requireCustomerOwnsRequest(requestId);

        // Chỉ cho đánh giá khi đơn đã hoàn thành
        if (owner.status == null || !owner.status.equalsIgnoreCase("COMPLETED")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ đơn đã hoàn thành mới được đánh giá"
            );
        }

        if (owner.providerId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn chưa được gán nhà cung cấp"
            );
        }

        int ratingValue = body.getRating();
        if (ratingValue < 1 || ratingValue > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating phải từ 1 đến 5"
            );
        }

        String comment = body.getComment();

        Review review = reviewRepository.findByRequestId(owner.requestId)
                .orElseGet(Review::new);

        review.setRequestId(owner.requestId);
        review.setCustomerId(owner.customerId);
        review.setProviderId(owner.providerId);
        review.setRating(ratingValue);
        review.setComment(comment);

        Review saved = reviewRepository.save(review);

        RatingResponseDTO dto = new RatingResponseDTO();
        dto.setRating(saved.getRating());
        dto.setComment(saved.getComment());
        if (saved.getCreatedAt() != null) {
            dto.setCreatedAt(dtf.format(saved.getCreatedAt()));
        }
        return dto;
    }

    // ================== DTO DETAIL ==================

    public static class DetailDTO {
        public Integer requestId;
        public String status;
        public String paymentStatus;
        public String paymentType;
        public String totalCostFormatted;
        public String depositFormatted;
        public String paidAtFormatted;

        public Integer contractId;
        public String contractStatus;
        public String contractSignedAtFormatted;
        public String contractAckAtFormatted;

        public static DetailDTO from(ServiceRequest r, Contract c, DateTimeFormatter dtf) {
            DetailDTO dto = new DetailDTO();
            dto.requestId = r.getRequestId();
            dto.status = r.getStatus();

            // Ưu tiên paymentStatus lưu trong DB nếu có
            String rawPaymentStatus = r.getPaymentStatus();
            if (rawPaymentStatus != null && !rawPaymentStatus.isBlank()) {
                dto.paymentStatus = rawPaymentStatus;
            } else {
                // Fallback: suy ra từ status của request (để các đơn cũ vẫn hiển thị đúng)
                String s = dto.status == null ? "" : dto.status.toLowerCase();
                if ("paid".equals(s) || "completed".equals(s)) {
                    dto.paymentStatus = "PAID";
                } else if ("ready_to_pay".equals(s)) {
                    dto.paymentStatus = "READY_TO_PAY";
                } else {
                    dto.paymentStatus = null; // để FE hiển thị "—"
                }
            }

            dto.paymentType = r.getPaymentType();

            if (r.getTotalCost() != null) {
                dto.totalCostFormatted = String.format("%,.0f đ", r.getTotalCost());
            }

            if (r.getDepositAmount() != null) {
                dto.depositFormatted = String.format("%,.0f đ", r.getDepositAmount());
            }

            if (r.getPaidAt() != null) {
                dto.paidAtFormatted = dtf.format(r.getPaidAt());
            }

            if (c != null) {
                dto.contractId = c.getContractId();
                dto.contractStatus = c.getStatus();

                if (c.getSignedAt() != null) {
                    dto.contractSignedAtFormatted = dtf.format(c.getSignedAt());
                }

                if (c.getAcknowledgedAt() != null) {
                    dto.contractAckAtFormatted = dtf.format(c.getAcknowledgedAt());
                }
            }

            return dto;
        }
    }
}
