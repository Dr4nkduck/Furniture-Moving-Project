package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderDTO;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProviderController {
    private final ProviderRepository providerRepository;

    public ProviderController(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }
    @GetMapping("")
    public String providerHome() { return "provider";}  // => providers.html

    @GetMapping("/provider/dashboard")
    public String dashboard() {
        return "provider/dashboard";
    }

        
    @GetMapping("/provider/services")
    public String services() {
        return "provider/services";
    }

    @GetMapping("/provider/orders")
    public String orders() {
        return "provider/orders";
    }

    @GetMapping("/provider/orders/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) { // PV-004/005
        model.addAttribute("orderId", id);
        return "provider/order-detail";
    }
}

