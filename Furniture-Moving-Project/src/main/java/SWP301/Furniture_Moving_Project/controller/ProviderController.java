package SWP301.Furniture_Moving_Project.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ProviderController {

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
}

