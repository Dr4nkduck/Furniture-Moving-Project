package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.CancellationRequest;
import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.CancellationRequestRepository;
import SWP301.Furniture_Moving_Project.repository.ContractRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Locale;

@Controller
@RequestMapping("/customer")
public class CustomerRequestController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ContractRepository contractRepository;
    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final CancellationRequestRepository cancellationRequestRepository;

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale LOCALE_VN = new Locale("vi", "VN");

    public CustomerRequestController(ServiceRequestRepository serviceRequestRepository,
                                     ContractRepository contractRepository,
                                     JdbcTemplate jdbc,
                                     UserRepository userRepository,
                                     CancellationRequestRepository cancellationRequestRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.contractRepository = contractRepository;
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.cancellationRequestRepository = cancellationRequestRepository;
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return null;
        BigDecimal v = amount.setScale(0, RoundingMode.HALF_UP);
        NumberFormat nf = NumberFormat.getInstance(LOCALE_VN);
        return nf.format(v) + " đ";
    }

    /**
     * Lấy customer_id tương ứng user đang login
     * users.username -> customers.customer_id
     */
    private Integer getCurrentCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String username = auth.getName();

        try {
            return jdbc.queryForObject(
                    """
                    SELECT c.customer_id
                    FROM customers c
                    JOIN users u ON u.user_id = c.user_id
                    WHERE u.username = ?
                    """,
                    Integer.class,
                    username
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không tìm thấy hồ sơ khách hàng cho tài khoản hiện tại"
            );
        }
    }

    // =========================
    // LIST: /customer/requests
    // =========================
    @GetMapping("/requests")
    public String viewMyRequests(
            @RequestParam(value = "status", required = false) String status,
            Model model
    ) {
        // Add login info for navbar
        addLoginInfo(model);

        Integer customerId = getCurrentCustomerId();

        List<ServiceRequest> requests;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            requests = serviceRequestRepository.findByCustomerIdAndStatus(customerId, status);
        } else {
            requests = serviceRequestRepository.findByCustomerId(customerId);
        }

        // Lấy toàn bộ contract tương ứng (tránh N+1)
        Map<Integer, Contract> contractsMap = Collections.emptyMap();
        if (!requests.isEmpty()) {
            Set<Integer> contractIds = requests.stream()
                    .map(ServiceRequest::getContractId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!contractIds.isEmpty()) {
                List<Contract> contracts = contractRepository.findAllById(contractIds);
                contractsMap = contracts.stream()
                        .collect(Collectors.toMap(Contract::getContractId, Function.identity()));
            }
        }

        model.addAttribute("requests", requests);
        model.addAttribute("selectedStatus", status == null ? "ALL" : status);
        model.addAttribute("contractsMap", contractsMap);

        return "customer/requests";
    }

    // =========================
    // DETAIL: /customer/requests/{id}
    // =========================
    @GetMapping("/requests/{id}")
    public String viewRequestDetail(@PathVariable("id") Integer requestId, Model model) {
        // Add login info for navbar
        addLoginInfo(model);

        Integer customerId = getCurrentCustomerId();

        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!customerId.equals(request.getCustomerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Contract contract = null;
        if (request.getContractId() != null) {
            contract = contractRepository.findById(request.getContractId()).orElse(null);
        }

        // Giai đoạn 1: hủy trực tiếp nếu đơn còn ở trạng thái cho phép
        boolean canCancel = request.isCancellableByCustomer();

        // Đã gửi yêu cầu hủy chưa? (giai đoạn 2)
        boolean hasPendingCancel = cancellationRequestRepository
                .existsByServiceRequestIdAndStatus(
                        requestId,
                        CancellationRequest.STATUS_REQUESTED
                );

        // Giai đoạn 2:
        // - Không còn được hủy trực tiếp nữa (canCancel == false)
        // - Đơn vẫn nằm trong "vùng cho phép yêu cầu hủy" theo rule entity
        // - Và chưa có yêu cầu hủy nào đang chờ duyệt
        boolean canRequestCancel =
                !canCancel
                        && request.isCancellationRequestAllowedByCustomer()
                        && !hasPendingCancel;

        // ==== xác định ai là người hủy (nếu đã cancelled) ====
        String cancelledBy = request.getCancelledBy();
        String cb = cancelledBy == null ? "" : cancelledBy.trim().toUpperCase();

        boolean cancelledByCustomer = "CUSTOMER".equals(cb);
        boolean cancelledByProvider = "PROVIDER".equals(cb);
        boolean cancelledByAdmin    = "ADMIN".equals(cb);

        model.addAttribute("request", request);
        model.addAttribute("contract", contract);
        model.addAttribute("canCancel", canCancel);
        model.addAttribute("canRequestCancel", canRequestCancel);
        model.addAttribute("hasPendingCancel", hasPendingCancel);

        // ai hủy
        model.addAttribute("cancelledBy", cancelledBy);
        model.addAttribute("cancelledByCustomer", cancelledByCustomer);
        model.addAttribute("cancelledByProvider", cancelledByProvider);
        model.addAttribute("cancelledByAdmin", cancelledByAdmin);

        return "customer/request-detail";
    }

    /**
     * API cho trang chi tiết: trả về trạng thái hiện tại (dùng cho polling realtime)
     * GET /customer/requests/{id}/status
     */
    @GetMapping("/requests/{id}/status")
    @ResponseBody
    public Map<String, Object> getRequestStatus(@PathVariable("id") Integer requestId) {
        Integer customerId = getCurrentCustomerId();

        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!customerId.equals(request.getCustomerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // === flag cho nút Hủy / Yêu cầu hủy ===
        boolean canCancel = request.isCancellableByCustomer();

        boolean hasPendingCancel = cancellationRequestRepository
                .existsByServiceRequestIdAndStatus(
                        requestId,
                        CancellationRequest.STATUS_REQUESTED
                );

        boolean canRequestCancel =
                !canCancel
                        && request.isCancellationRequestAllowedByCustomer()
                        && !hasPendingCancel;

        // === thông tin hợp đồng (nếu có) ===
        Contract contract = null;
        if (request.getContractId() != null) {
            contract = contractRepository.findById(request.getContractId()).orElse(null);
        }

        // === xác định bên hủy ===
        String cancelledBy = request.getCancelledBy();
        String cb = cancelledBy == null ? "" : cancelledBy.trim().toUpperCase();

        String cancelledByDisplay;
        if ("CUSTOMER".equals(cb)) {
            cancelledByDisplay = "Khách hàng";
        } else if ("PROVIDER".equals(cb)) {
            cancelledByDisplay = "Đơn vị vận chuyển";
        } else if ("ADMIN".equals(cb)) {
            cancelledByDisplay = "Quản trị viên";
        } else if (cb.isEmpty()) {
            cancelledByDisplay = null;
        } else {
            cancelledByDisplay = "Không xác định";
        }

        Map<String, Object> res = new HashMap<>();

        // ---- trạng thái request / payment ----
        res.put("status", request.getStatus());                 // pending / ready_to_pay / ...
        res.put("paymentStatus", request.getPaymentStatus());   // PENDING / PAID / FAILED / ...
        res.put("paymentType", request.getPaymentType());       // DEPOSIT_20 / FULL / null

        // ---- tiền & thời điểm ----
        res.put("paidAt", request.getPaidAt());
        res.put("paidAtFormatted",
                request.getPaidAt() != null ? request.getPaidAt().format(DATETIME_FMT) : null);

        res.put("totalCostFormatted", formatMoney(request.getTotalCost()));
        res.put("depositFormatted", formatMoney(request.getDepositAmount()));

        // ---- contract ----
        res.put("contractId", request.getContractId());
        if (contract != null) {
            res.put("contractStatus", contract.getStatus());
            res.put("contractSignedAtFormatted",
                    contract.getSignedAt() != null ? contract.getSignedAt().format(DATETIME_FMT) : null);
            res.put("contractAckAtFormatted",
                    contract.getAcknowledgedAt() != null ? contract.getAcknowledgedAt().format(DATETIME_FMT) : null);
        }

        // ---- flag button ----
        res.put("canCancel", canCancel);
        res.put("canRequestCancel", canRequestCancel);
        res.put("hasPendingCancel", hasPendingCancel);

        // ---- thông tin hủy trực tiếp ----
        res.put("cancelReason", request.getCancelReason());
        res.put("cancelledBy", cancelledBy);
        res.put("cancelledByDisplay", cancelledByDisplay);
        res.put("cancelledAt", request.getCancelledAt());
        res.put("cancelledAtFormatted",
                request.getCancelledAt() != null ? request.getCancelledAt().format(DATETIME_FMT) : null);

        return res;
    }

    /**
     * Add login info to model for navbar
     */
    private void addLoginInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            model.addAttribute("isLoggedIn", true);

            String username = auth.getName();
            if (username != null) {
                userRepository.findByUsername(username).ifPresent(u -> {
                    model.addAttribute("currentUser", u);
                });
            }
        } else {
            model.addAttribute("isLoggedIn", false);
        }
    }
}
