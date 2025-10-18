package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    @GetMapping("/login")
    public String login(Boolean error, Model model) {
        // Thymeleaf sẽ đọc param ?error
        return "accountmanage/login";
    }
}
