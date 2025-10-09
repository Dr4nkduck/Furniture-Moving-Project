package SWP301.Furniture_Moving_Project.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AccountManagementPageController {

    @GetMapping("/accountmanagement")
    public String accountManagementPage() {
        // resolves src/main/resources/templates/accountmanagement.html
        return "accountmanagement";
    }
}
