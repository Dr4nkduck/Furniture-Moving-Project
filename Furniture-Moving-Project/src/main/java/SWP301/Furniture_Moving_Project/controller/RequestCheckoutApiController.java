package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.CheckoutRequest;
import SWP301.Furniture_Moving_Project.dto.RequestCreatedResponse;
import SWP301.Furniture_Moving_Project.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
public class RequestCheckoutApiController {

    private final CheckoutService checkoutService;

    public RequestCheckoutApiController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<RequestCreatedResponse> create(@RequestBody CheckoutRequest req) {
        Integer requestId = checkoutService.createPendingRequest(req);
        return ResponseEntity.ok(new RequestCreatedResponse(requestId, "pending"));
    }
}

