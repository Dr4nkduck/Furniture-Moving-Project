package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderOrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderSummaryDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderUpdateStatusDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.ProviderOrderService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;


import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProviderApiController {

    private final ProviderRepository providerRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ProviderOrderService providerOrderService;

    public ProviderApiController(ProviderRepository providerRepository,
                                 ServiceRequestRepository serviceRequestRepository,
                                 ProviderOrderService providerOrderService) {
        this.providerRepository = providerRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.providerOrderService = providerOrderService;
    }

    /** (Tuỳ chọn) Endpoint nhẹ cho dropdown: /api/providers/available */
    @GetMapping("/available")
    public List<Map<String, Object>> getAvailableProvidersLight() {
        return providerRepository.findAvailableProvidersLight();
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "verified") String status) {
        var data = providerRepository
                .findByVerificationStatusOrderByCompanyNameAsc(status)
                .stream()
                .map(p -> Map.of(
                        "providerId", p.getProviderId(),
                        "companyName", p.getCompanyName(),
                        "rating", p.getRating()
                ))
                .collect(Collectors.toList());
        return Map.of("success", true, "data", data);
    }

    // SEARCH: GET /api/providers/search?name=...
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam(required = false) String name) {
        var list = (name == null || name.isBlank())
                ? providerRepository.findAll()
                : providerRepository.findByCompanyNameContainingIgnoreCase(name.trim());

        var data = list.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("providerId", p.getProviderId());
            m.put("companyName", p.getCompanyName());
            m.put("rating", p.getRating());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // AVAILABILITY: GET /api/providers/availability?date=YYYY-MM-DD
    @GetMapping("/availability")
    public ResponseEntity<Map<String, Object>> availability(@RequestParam("date") String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        List<String> busyStatuses = Arrays.asList("assigned", "in_progress");

        var data = new ArrayList<Map<String, Object>>();
        for (Provider p : providerRepository.findAll()) {
            long busyCount = serviceRequestRepository
                    .countByProviderIdAndPreferredDateAndStatusIn(
                            p.getProviderId(), date, busyStatuses);

            Map<String, Object> m = new HashMap<>();
            m.put("providerId", p.getProviderId());
            m.put("companyName", p.getCompanyName());
            m.put("rating", p.getRating());
            m.put("available", busyCount == 0);
            m.put("busyCount", busyCount);
            data.add(m);
        }

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // PV-003: List/Search orders của provider
    // GET /api/providers/{providerId}/orders?status=...&q=...
    @GetMapping("/{providerId}/orders")
    public List<ProviderOrderSummaryDTO> listOrders(@PathVariable Integer providerId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false, name = "q") String q) {
        return providerOrderService.listOrders(providerId, status, q);
    }

    // GET /api/providers/{providerId}/orders/{orderId}
    @GetMapping("/{providerId}/orders/{orderId}")
    public ProviderOrderDetailDTO getOrderDetail(@PathVariable Integer providerId,
                                                 @PathVariable Integer orderId) {
        return providerOrderService.getOrderDetail(providerId, orderId);
    }

    // PV-004: Cập nhật trạng thái đơn
    @PutMapping("/{providerId}/orders/{orderId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOrderStatus(@PathVariable Integer providerId,
                                  @PathVariable Integer orderId,
                                  @RequestBody ProviderOrderUpdateStatusDTO body) {
        providerOrderService.updateOrderStatus(providerId, orderId, body.getStatus(), body.getCancelReason());
    }

    // === Nút "Xác nhận đã thanh toán" cho Provider ===
    // Provider đã tự kiểm tra sao kê ngân hàng, sau đó bấm nút trên UI.
    // Chỉ đổi trạng thái order sang "paid" nếu:
    //  - Đơn thuộc providerId này
    //  - Đơn đang ở trạng thái "ready_to_pay"
    @PostMapping(path = {"/api/providers/{providerId}/orders/{orderId}/confirm-payment", "/{providerId}/orders/{orderId}/confirm-payment"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPayment(@PathVariable Integer providerId,
                               @PathVariable Integer orderId) {
        providerOrderService.confirmPayment(providerId, orderId);
    }
}
