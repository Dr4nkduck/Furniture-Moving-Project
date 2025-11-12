// src/main/java/SWP301/Furniture_Moving_Project/controller/ProviderController.java
package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/provider") // <— KHÁC với /api/providers
public class ProviderController {

    private final ProviderRepository providerRepository;

    public ProviderController(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @GetMapping("")
    public String providerHome() { return "provider"; }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "provider/dashboard";
    }

    @GetMapping("/services")
    public String services() {
        return "provider/services";
    }

    @GetMapping("/orders")
    public String orders() {
        return "provider/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("orderId", id);
        return "provider/order-detail";
    }
}
