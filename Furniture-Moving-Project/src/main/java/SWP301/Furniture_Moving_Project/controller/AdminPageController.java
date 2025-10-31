package SWP301.Furniture_Moving_Project.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageController {

    @GetMapping("/accountmanagement")
    public String accountManagementPage() {
        // Template is under templates/admin/accountmanagement.html
        return "admin/accountmanagement";
    }

    @GetMapping("/customer-trends")
    public String customer_trends() {
        // khớp với file templates/admin/dashboard.html
        return "admin/customer-trends";
    }

    @GetMapping("/provider-stats")
    public String provier_stat() {
        // khớp với file templates/admin/dashboard.html
        return "admin/provider-stats";
    }

}
