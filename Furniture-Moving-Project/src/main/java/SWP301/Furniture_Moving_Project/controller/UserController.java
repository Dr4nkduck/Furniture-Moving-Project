package SWP301.Furniture_Moving_Project.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {

    @GetMapping("/user")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String userHome() {
        return "user";
    }

    // Trang Báo giá bằng AI
    @GetMapping("/user/ai-quote")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String aiQuote() {
        return "user/ai-quote"; // -> templates/user/ai-quote.html
    }
}
