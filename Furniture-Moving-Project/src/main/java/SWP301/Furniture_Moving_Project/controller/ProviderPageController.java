package SWP301.Furniture_Moving_Project.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProviderPageController {

    @GetMapping("/provider/services")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public String providerServicesPage(@RequestParam(required = false) Integer providerId) {
        // Trang dùng query ?providerId=... để JS gọi API
        return "provider/services";
    }
}
