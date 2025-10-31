package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")

public class AdminController {

    @GetMapping("/dashboard")
    public String dashboard() {
        // khớp với file templates/admin/dashboard.html
        return "admin/dashboard";
    }
}
