package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.RatingRequestDTO;
import SWP301.Furniture_Moving_Project.dto.RatingResponseDTO;
import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.model.Review;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
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

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer/request")
public class CustomerRequestDetailApiController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ContractRepository contractRepository;
    private final JdbcTemplate jdbc;
    private final ReviewRepository reviewRepository;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CustomerRequestDetailApiController(
            ServiceRequestRepository serviceRequestRepository,
            ContractRepository contractRepository,
            JdbcTemplate jdbc,
            ReviewRepository reviewRepository
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.contractRepository = contractRepository;
        this.jdbc = jdbc;
        this.reviewRepository = reviewRepository;
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

    /** Thông tin đơn dùng riêng cho rating */
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

            // ✅ Ưu tiên paymentStatus lưu trong DB nếu có (PaymentServiceImpl đã set "PAID")
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
