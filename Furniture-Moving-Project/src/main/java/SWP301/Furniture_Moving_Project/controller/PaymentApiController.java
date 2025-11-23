package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.PaymentInitResponse;
import SWP301.Furniture_Moving_Project.dto.PaymentStatusResponse;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.PaymentService;
import SWP301.Furniture_Moving_Project.service.ServiceRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentApiController {

    private final PaymentService paymentService;
    private final ServiceRequestService serviceRequestService;
    private final ServiceRequestRepository serviceRequestRepository;

    public PaymentApiController(PaymentService paymentService,
                                ServiceRequestService serviceRequestService,
                                ServiceRequestRepository serviceRequestRepository) {
        this.paymentService = paymentService;
        this.serviceRequestService = serviceRequestService;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    /**
     * Khởi tạo phiên thanh toán (VietQR / VNPay)
     * Trả về JSON cho payment.js
     */
    @PostMapping("/{requestId}/init")
    public ResponseEntity<PaymentInitResponse> init(@PathVariable Integer requestId,
                                                    @RequestParam(required = false, defaultValue = "FULL") String paymentType) {

        PaymentInitResponse response;

        if (paymentService instanceof SWP301.Furniture_Moving_Project.service.impl.PaymentServiceImpl impl) {
            response = impl.initPayment(requestId, paymentType);
        } else {
            response = paymentService.initPayment(requestId);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy trạng thái thanh toán hiện tại (JS gọi mỗi 5s)
     */
    @GetMapping("/{requestId}/status")
    public PaymentStatusResponse status(@PathVariable Integer requestId) {
        return paymentService.getPaymentStatus(requestId);
    }

    /**
     * Endpoint callback / manual verify: đánh dấu thanh toán thành công
     * - Dùng khi gateway gọi lại (IPN) hoặc test local
     * - POST /payment/{id}/verify?amount=xxx&paymentType=FULL
     */
    @PostMapping("/{requestId}/verify")
    public Map<String, Object> verify(@PathVariable Integer requestId,
                                      @RequestParam BigDecimal amount,
                                      @RequestParam(defaultValue = "FULL") String paymentType) {

        ServiceRequest updated = serviceRequestService.markAsPaid(requestId, amount, paymentType);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "PAID");
        resp.put("requestStatus", updated.getStatus());
        resp.put("paidAt", updated.getPaidAt());
        return resp;
    }

    /**
     * Endpoint test nội bộ: fake xác nhận thanh toán
     * - POST /payment/{id}/fake-paid?amount=xxx&paymentType=FULL
     */
    @PostMapping("/{requestId}/fake-paid")
    public Map<String, Object> fakePaid(@PathVariable Integer requestId,
                                        @RequestParam BigDecimal amount,
                                        @RequestParam(defaultValue = "FULL") String paymentType) {

        ServiceRequest updated = serviceRequestService.markAsPaid(requestId, amount, paymentType);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "PAID");
        resp.put("requestStatus", updated.getStatus());
        resp.put("paidAt", updated.getPaidAt());
        resp.put("note", "TEST ONLY - Không dùng cho môi trường thật");
        return resp;
    }
}
