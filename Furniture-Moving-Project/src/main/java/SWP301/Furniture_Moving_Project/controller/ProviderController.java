// src/main/java/SWP301/Furniture_Moving_Project/controller/ProviderController.java
package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.model.Provider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/provider")
public class ProviderController {

    private final ProviderRepository providerRepository;

    public ProviderController(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @GetMapping({"", "/", "/home"})
    public String home() {
        return "provider/home";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "provider/dashboard";
    }

    @GetMapping("/services")
    public String services(Model model, Authentication auth) {
        // Lấy username đang đăng nhập
        String username = auth != null ? auth.getName() : null;

        // Tìm provider theo username
        Integer providerId = providerRepository
                .findProviderIdByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy provider cho user: " + username));

        // Nếu muốn show companyName trên navbar:
        Optional<Provider> pOpt = providerRepository.findById(providerId);
        pOpt.ifPresent(p -> model.addAttribute("companyName", p.getCompanyName()));

        // Đưa providerId xuống view để <meta> dùng
        model.addAttribute("providerId", providerId);

        return "provider/services";
    }

    @GetMapping("/orders")
    public String orders(Model model, Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        Integer providerId = providerRepository
                .findProviderIdByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy provider cho user: " + username));
        model.addAttribute("providerId", providerId);
        return "provider/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("orderId", id);
        return "provider/order-detail";
    }

    // ✅ Trang xem review khách đánh giá provider
    @GetMapping("/reviews")
    public String reviews(Model model, Authentication auth) {
        String username = auth != null ? auth.getName() : null;

        Integer providerId = providerRepository
                .findProviderIdByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy provider cho user: " + username));

        // companyName để show trên navbar/sidebar nếu cần
        Optional<Provider> pOpt = providerRepository.findById(providerId);
        pOpt.ifPresent(p -> model.addAttribute("companyName", p.getCompanyName()));

        model.addAttribute("providerId", providerId);

        return "provider/reviews"; // trỏ tới templates/provider/reviews.html
    }
}
