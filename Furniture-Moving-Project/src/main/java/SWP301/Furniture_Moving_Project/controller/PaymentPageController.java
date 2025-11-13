package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.ServiceRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class PaymentPageController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final JdbcTemplate jdbc;
    private final UserRepository userRepository; // ✅ thêm repository để truy user

    public PaymentPageController(ServiceRequestRepository serviceRequestRepository,
                                 JdbcTemplate jdbcTemplate,
                                 UserRepository userRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.jdbc = jdbcTemplate;
        this.userRepository = userRepository;
    }

    @GetMapping("/payment/{id}")
    public String viewPayment(@PathVariable("id") Integer requestId, Model model) {
        // ✅ Thêm thông tin đăng nhập trước
        addLoginInfo(model);

        ServiceRequest sr = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vận chuyển #" + requestId));

        // ---- Thông tin cơ bản khớp HTML
        BigDecimal amount = sr.getTotalCost();
        LocalDateTime createdAt = sr.getRequestDate();
        LocalDate expectedDate = sr.getPreferredDate();
        String status = sr.getStatus();

        int itemCount  = queryInt("SELECT COUNT(*) FROM dbo.furniture_items  WHERE request_id = ?", requestId);
        int imageCount = queryInt("SELECT COUNT(*) FROM dbo.request_images   WHERE request_id = ?", requestId);

        String providerCompanyName = null;
        if (sr.getProviderId() != null) {
            providerCompanyName = queryString("SELECT company_name FROM dbo.providers WHERE provider_id = ?", sr.getProviderId());
        }
        if (providerCompanyName == null || providerCompanyName.isBlank()) providerCompanyName = "Đang xử lý";

        // ---- LẤY ĐỊA CHỈ
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

        // ---- Đẩy model cho payment.html
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
                && !"anonymousUser".equals(auth.getPrincipal())) {
            model.addAttribute("isLoggedIn", true);

            Object principal = auth.getPrincipal();
            String username = null;
            try {
                username = (String) principal.getClass().getMethod("getUsername").invoke(principal);
            } catch (Exception ignored) {
            }

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
