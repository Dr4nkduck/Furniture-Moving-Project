package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProviderPageController {

    @GetMapping("/provider")
    public String providerRoot() {
        return "redirect:/provider/dashboard";
    }

    @GetMapping("/provider/dashboard")
    public String dashboard() {
        return "provider/dashboard";   // -> templates/provider/dashboard.html
    }

    @GetMapping("/provider/services")
    public String services() {
        return "provider/services";    // -> templates/provider/services.html
    }

    @GetMapping("/provider/orders")
    public String orders() {
        return "provider/orders";      // -> templates/provider/orders.html
    }
}
