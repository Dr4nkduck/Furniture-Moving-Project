package SWP301.Furniture_Moving_Project.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ProviderController {

    @GetMapping("")
    public String providerHome() { return "provider";}  // => providers.html

    @GetMapping("/provider/dashboard")
    public String dashboard() {
        return "provider/dashboard";
    }

    @GetMapping("/provider/services")
    public String services() {                    // PV-002
        return "provider/services";
    }

    @GetMapping("/provider/orders")
    public String orders() {                      // PV-003
        return "provider/orders";
    }

    @GetMapping("/provider/orders/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) { // PV-004/005
        model.addAttribute("orderId", id);
        return "provider/order-detail";
    }
}

