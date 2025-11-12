// src/main/java/SWP301/Furniture_Moving_Project/controller/ProviderApiController.java
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

    // ---------------------------------------------------------------------
    // SEARCH PROVIDERS
    // GET /api/providers/search?name=...
    // Trả về list Map để không phụ thuộc constructor của ProviderDTO.
    // ---------------------------------------------------------------------
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam(required = false) String name) {
        var list = (name == null || name.isBlank())
                ? providerRepository.findAll()
                : providerRepository.findByCompanyNameContainingIgnoreCase(name.trim());

        var data = list.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("providerId", p.getProviderId());
            m.put("companyName", p.getCompanyName());
            m.put("rating", p.getRating()); // BigDecimal hoặc null
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // ---------------------------------------------------------------------
    // AVAILABILITY
    // GET /api/providers/availability?date=YYYY-MM-DD
    // ---------------------------------------------------------------------
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

    // ---------------------------------------------------------------------
    // ORDERS (PV-003/004)
    // ---------------------------------------------------------------------

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

    // PUT /api/providers/{providerId}/orders/{orderId}/status
    @PutMapping("/{providerId}/orders/{orderId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOrderStatus(@PathVariable Integer providerId,
                                  @PathVariable Integer orderId,
                                  @RequestBody ProviderOrderUpdateStatusDTO body) {
        providerOrderService.updateOrderStatus(providerId, orderId, body.getStatus(), body.getCancelReason());
    }
}
