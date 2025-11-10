package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class PaymentPageController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final JdbcTemplate jdbc; // dùng để đếm nhanh, không cần entity

    public PaymentPageController(ServiceRequestRepository serviceRequestRepository,
                                 JdbcTemplate jdbcTemplate) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.jdbc = jdbcTemplate;
    }

    @GetMapping("/payment/{id}")
    public String viewPayment(@PathVariable("id") Integer requestId, Model model) {
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vận chuyển #" + requestId));

        // ---- 1) Thông tin cơ bản
        BigDecimal amount = sr.getTotalCost();                     // Giá cần thanh toán
        LocalDateTime createdAt = sr.getRequestDate();             // Ngày tạo đơn
        LocalDate expectedDate = sr.getPreferredDate();            // Ngày vận chuyển dự kiến

        // ---- 2) Số lượng đồ & số ảnh minh họa (đếm bằng SQL nhanh gọn)
        int itemCount = queryInt("SELECT COUNT(*) FROM dbo.furniture_items WHERE request_id = ?",
                requestId);
        int imageCount = queryInt("SELECT COUNT(*) FROM dbo.request_images WHERE request_id = ?",
                requestId);

        // ---- 3) Tên nhà cung cấp (nếu chưa có provider_id thì hiển thị 'Đang xử lý')
        String providerCompany = null;
        if (sr.getProviderId() != null) {
            providerCompany = queryString("""
                    SELECT p.company_name FROM dbo.providers p WHERE p.provider_id = ?
                    """, sr.getProviderId());
        }
        if (providerCompany == null || providerCompany.isBlank()) {
            providerCompany = "Đang xử lý";
        }

        // ---- 4) Snapshot địa chỉ nhận / giao (nếu có bảng request_addresses)
        String pickupText = queryAddressSnapshot(requestId, "PICKUP");
        String deliveryText = queryAddressSnapshot(requestId, "DELIVERY");

        // ---- 5) Đẩy lên model đúng tên mà template đang dùng
        model.addAttribute("requestId", requestId);
        model.addAttribute("amount", amount);                           // dùng server-side & JS
        model.addAttribute("createdAt", createdAt);                     // dd/MM/yyyy HH:mm trong html
        model.addAttribute("expectedDate", expectedDate);               // dd/MM/yyyy trong html
        model.addAttribute("itemCount", itemCount);
        model.addAttribute("imageCount", imageCount);
        model.addAttribute("providerCompanyName", providerCompany);
        model.addAttribute("pickupText", pickupText);
        model.addAttribute("deliveryText", deliveryText);

        // hiển thị trạng thái hiện tại lên badge (nếu bạn có)
        model.addAttribute("status", sr.getStatus());

        return "payment/payment"; // đường dẫn tới payment.html của bạn (đổi cho đúng)
    }

    // ================= helpers =================

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

    private String queryAddressSnapshot(Integer requestId, String type) {
        // Nếu bạn chưa có bảng request_addresses thì trả null để block bị ẩn
        String sql = """
            SELECT TOP(1)
                   CONCAT(street_address, ', ', 
                          COALESCE(city,''), CASE WHEN city IS NULL OR city='' THEN '' ELSE ', ' END,
                          COALESCE(state,''), ' ',
                          COALESCE(zip_code,'')) AS full_addr
            FROM dbo.request_addresses
            WHERE request_id = ? AND address_type = ?
            ORDER BY request_address_id
            """;
        try {
            String s = jdbc.queryForObject(sql, String.class, requestId, type);
            return (s == null || s.isBlank()) ? null : s;
        } catch (Exception e) {
            return null;
        }
    }
}
