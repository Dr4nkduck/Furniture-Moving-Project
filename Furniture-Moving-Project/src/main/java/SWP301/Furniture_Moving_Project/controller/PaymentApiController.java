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
    public ResponseEntity<PaymentInitResponse> init(@PathVariable Integer requestId,
                                                    @RequestParam(required = false, defaultValue = "FULL") String paymentType) {
        if (paymentService instanceof SWP301.Furniture_Moving_Project.service.impl.PaymentServiceImpl) {
            return ResponseEntity.ok(((SWP301.Furniture_Moving_Project.service.impl.PaymentServiceImpl) paymentService)
                .initPayment(requestId, paymentType));
        }
        return ResponseEntity.ok(paymentService.initPayment(requestId));
    }

    @GetMapping("/{requestId}/status")
    public PaymentStatusResponse status(@PathVariable Integer requestId) {
        return paymentService.getPaymentStatus(requestId);
    }
}
