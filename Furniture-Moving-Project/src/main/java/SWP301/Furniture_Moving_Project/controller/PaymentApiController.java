package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.PaymentInitResponse;
import SWP301.Furniture_Moving_Project.dto.PaymentStatusResponse;
import SWP301.Furniture_Moving_Project.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentApiController {
    private final PaymentService paymentService;

    public PaymentApiController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{requestId}/init")
    public ResponseEntity<PaymentInitResponse> init(@PathVariable Integer requestId) {
        return ResponseEntity.ok(paymentService.initPayment(requestId));
    }

    @GetMapping("/{requestId}/status")
    public PaymentStatusResponse status(@PathVariable Integer requestId) {
        return paymentService.getPaymentStatus(requestId);
    }
}
