package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPaymentPageController {

    @GetMapping("/payments")
    public String paymentConfirmPage() {
        // templates/admin/payment-confirm.html
        return "admin/payment-confirm";
    }
}
