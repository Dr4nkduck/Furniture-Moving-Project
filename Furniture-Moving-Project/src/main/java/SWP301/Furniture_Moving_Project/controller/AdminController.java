// AdminController.java
package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @GetMapping("/dashboard")
    public String dashboard() {
        // Nếu bạn muốn dùng trang trong thư mục superadmin,
        // có thể tạm tái sử dụng:
        return "superadmin/dashboard";
        // Khuyến nghị: tạo templates/admin/dashboard.html riêng cho admin.
    }
}
