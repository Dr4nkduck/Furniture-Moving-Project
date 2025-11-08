package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.config.PaymentProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Controller
public class PaymentPageController {

    private final ServiceRequestRepository requestRepo;
    private final PaymentProperties props;

    public PaymentPageController(ServiceRequestRepository requestRepo,
                                 PaymentProperties props) {
        this.requestRepo = requestRepo;
        this.props = props;
    }

    @GetMapping("/payment")
    public String payment(@RequestParam("rid") Integer rid, Model model) {
        ServiceRequest sr = requestRepo.findById(rid)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + rid));

        String orderCode = String.format("SR%06d", sr.getRequestId());
        BigDecimal amount = sr.getTotalCost() == null ? BigDecimal.ZERO : sr.getTotalCost();

        model.addAttribute("rid", sr.getRequestId());
        model.addAttribute("orderCode", orderCode);
        model.addAttribute("amount", amount);
        model.addAttribute("preferredDate",
                sr.getPreferredDate() == null ? "-" : sr.getPreferredDate().format(DateTimeFormatter.ISO_DATE));

        // Bank info từ application.yml
        model.addAttribute("bankName", props.getBank().getName());
        model.addAttribute("bankAccountNo", props.getBank().getAccountNumber());
        model.addAttribute("bankAccountName", props.getBank().getAccountName());

        // (Optional) bạn có thể map pickup/delivery sau, tạm để rỗng
        model.addAttribute("pickup", "");
        model.addAttribute("delivery", "");

        return "payment/payment"; // → templates/payment/payment.html
    }
}
