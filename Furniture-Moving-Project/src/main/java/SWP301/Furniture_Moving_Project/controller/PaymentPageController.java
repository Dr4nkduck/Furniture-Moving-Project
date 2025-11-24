package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ContractRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class PaymentPageController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;

    public PaymentPageController(ServiceRequestRepository serviceRequestRepository,
                                 JdbcTemplate jdbcTemplate,
                                 UserRepository userRepository,
                                 ContractRepository contractRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.jdbc = jdbcTemplate;
        this.userRepository = userRepository;
        this.contractRepository = contractRepository;
    }

    @GetMapping("/payment/{id}")
    public String viewPayment(@PathVariable("id") Integer requestId, Model model) {
        // ✅ (0) Thêm thông tin đăng nhập cho navbar/template
        addLoginInfo(model);

        // ✅ (1) Lấy username từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn cần đăng nhập để truy cập thanh toán."
            );
        }
        String username = auth.getName();

        // ✅ (2) Chặn nếu đơn không thuộc user (giữ logic repo cũ)
        boolean allowed = serviceRequestRepository.canAccessPayment(requestId, username) == 1;
        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn chưa đủ điều kiện để thanh toán: đơn không thuộc bạn hoặc chưa được nhà vận chuyển ghi nhận."
            );
        }

        // ✅ (3) Hợp lệ rồi mới load dữ liệu đơn
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn vận chuyển #" + requestId
                ));

        String status = sr.getStatus();
        String paymentStatus = sr.getPaymentStatus(); // nếu có field này trong entity

        // 🔒 Nếu đã thanh toán rồi thì không cho quay lại màn thanh toán nữa
        if ("paid".equalsIgnoreCase(status)
                || "PAID".equalsIgnoreCase(paymentStatus)) {
            // Tuỳ bạn: quay về trang chi tiết yêu cầu
            return "redirect:/customer/requests/" + requestId;
        }

        // 🔒 (4) Kiểm tra hợp đồng gắn với đơn
        Integer contractId = sr.getContractId();
        if (contractId == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Đơn này chưa có hợp đồng hoàn chỉnh, không thể thanh toán."
            );
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Không tìm thấy hợp đồng tương ứng với đơn này."
                ));

        String contractStatus = contract.getStatus();

        // 🔒 (5) Chỉ cho thanh toán khi hợp đồng đã được xác nhận đầy đủ
        // Ở đây align với UI: status = "acknowledged" (Đơn vị vận chuyển đã xác nhận)
        if (contractStatus == null || !contractStatus.equalsIgnoreCase("acknowledged")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Hợp đồng chưa được xác nhận đầy đủ. Vui lòng xem và xác nhận hợp đồng trước khi thanh toán."
            );
        }

        // 🔒 (6) Đơn phải đang ở trạng thái sẵn sàng thanh toán
        if (!"ready_to_pay".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Đơn này chưa ở trạng thái sẵn sàng thanh toán."
            );
        }

        // ---- Thông tin cơ bản khớp HTML ----
        BigDecimal amount = sr.getTotalCost();
        LocalDateTime createdAt = sr.getRequestDate();
        LocalDate expectedDate = sr.getPreferredDate();

        int itemCount  = queryInt("SELECT COUNT(*) FROM dbo.furniture_items  WHERE request_id = ?", requestId);
        int imageCount = queryInt("SELECT COUNT(*) FROM dbo.request_images   WHERE request_id = ?", requestId);

        String providerCompanyName = null;
        if (sr.getProviderId() != null) {
            providerCompanyName = queryString(
                    "SELECT company_name FROM dbo.providers WHERE provider_id = ?",
                    sr.getProviderId()
            );
        }
        if (providerCompanyName == null || providerCompanyName.isBlank()) {
            providerCompanyName = "Đang xử lý";
        }

        // ---- LẤY ĐỊA CHỈ ----
        String pickupText = queryString("""
                SELECT a.street_address
                FROM dbo.addresses a
                JOIN dbo.service_requests sr ON sr.pickup_address_id = a.address_id
                WHERE sr.request_id = ?
                """, requestId);
        String deliveryText = queryString("""
                SELECT a.street_address
                FROM dbo.addresses a
                JOIN dbo.service_requests sr ON sr.delivery_address_id = a.address_id
                WHERE sr.request_id = ?
                """, requestId);

        if (pickupText == null || pickupText.isBlank())     pickupText = "—";
        if (deliveryText == null || deliveryText.isBlank()) deliveryText = "—";

        // 🔹 Mã tham chiếu thanh toán dùng cho VietQR / sao kê ngân hàng: REQ(id)
        String paymentRef = "REQ" + requestId;

        // ---- Đẩy model cho payment.html ----
        model.addAttribute("requestId", requestId);
        model.addAttribute("amount", amount);
        model.addAttribute("createdAt", createdAt);
        model.addAttribute("expectedDate", expectedDate);
        model.addAttribute("itemCount", itemCount);
        model.addAttribute("imageCount", imageCount);
        model.addAttribute("providerCompanyName", providerCompanyName);
        model.addAttribute("pickupText", pickupText);
        model.addAttribute("deliveryText", deliveryText);
        model.addAttribute("status", status);
        model.addAttribute("paymentRef", paymentRef);

        return "payment/payment";
    }

    // ===== Helpers =====
    private int queryInt(String sql, Object... args) {
        try {
            Integer v = jdbc.queryForObject(sql, Integer.class, args);
            return v == null ? 0 : v;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private String queryString(String sql, Object... args) {
        try {
            return jdbc.queryForObject(sql, String.class, args);
        } catch (Exception e) {
            return null;
        }
    }

    /** ============================================================
     *  HÀM DÙNG CHUNG CHO NAVBAR (thêm biến isLoggedIn, currentUser)
     *  ============================================================ */
    private void addLoginInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            model.addAttribute("isLoggedIn", true);

            String username = auth.getName();
            if (username != null) {
                userRepository.findByUsername(username).ifPresent(u ->
                        model.addAttribute("currentUser", u)
                );
            }
        } else {
            model.addAttribute("isLoggedIn", false);
        }
    }
}
