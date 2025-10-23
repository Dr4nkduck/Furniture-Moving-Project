package SWP301.Furniture_Moving_Project.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountManagementPageController {

    @GetMapping("/accountmanagement")
    @PreAuthorize("hasRole('ADMIN')")
    public String page() {
        // resolves templates/admin/accountmanagement.html
        return "admin/accountmanagement";
    }
}
