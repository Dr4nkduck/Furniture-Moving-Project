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
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional(readOnly = true)
    public Map<String, Object> list(@RequestParam(required = false) String status) {
        // If no status specified, return all providers (for request form dropdown)
        // If status specified, filter by that status
        List<Map<String, Object>> data;

        if (status == null || status.isBlank()) {
            // Use native query to avoid lazy loading issues
            data = providerRepository.findAvailableProvidersLight();
        } else {
            // Filter by status using entity query
            data = providerRepository
                    .findByVerificationStatusOrderByCompanyNameAsc(status)
                    .stream()
                    .map(p -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("providerId", p.getProviderId());
                        m.put("companyName", p.getCompanyName());
                        m.put("rating", p.getRating());
                        return m;
                    })
                    .collect(Collectors.toList());
        }

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
    public ResponseEntity<?> updateOrderStatus(@PathVariable Integer providerId,
                                               @PathVariable Integer orderId,
                                               @RequestBody ProviderOrderUpdateStatusDTO body) {
        try {
            providerOrderService.updateOrderStatus(
                    providerId,
                    orderId,
                    body.getStatus(),
                    body.getCancelReason()
            );
            return ResponseEntity.noContent().build(); // 204
        } catch (IllegalStateException ex) {
            // Sai luồng trạng thái (ví dụ: completed -> in_progress)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "message", ex.getMessage()
                    ));
        } catch (IllegalArgumentException ex) {
            // Dữ liệu sai / không tìm thấy
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", ex.getMessage()
                    ));
        }
    }

    // === Nút "Xác nhận đã thanh toán" cho Provider ===
    // Provider đã tự kiểm tra sao kê ngân hàng, sau đó bấm nút trên UI.
    // Chỉ đổi trạng thái order sang "paid" nếu:
    //  - Đơn thuộc providerId này
    //  - Đơn đang ở trạng thái "ready_to_pay"
    @PostMapping("/{providerId}/orders/{orderId}/confirm-payment")
    public ResponseEntity<?> confirmPayment(@PathVariable Integer providerId,
                                            @PathVariable Integer orderId) {
        try {
            providerOrderService.confirmPayment(providerId, orderId);
            return ResponseEntity.noContent().build(); // 204
        } catch (IllegalStateException ex) {
            // Ví dụ: không ở trạng thái ready_to_pay
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "message", ex.getMessage()
                    ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", ex.getMessage()
                    ));
        }
    }

    // GET /api/providers/me - Lấy thông tin provider hiện tại
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalArgumentException("Chưa đăng nhập");
        }
        String username = auth.getName();
        Integer providerId = providerRepository
                .findProviderIdByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy provider cho user: " + username));

        return Map.of(
                "success", true,
                "providerId", providerId
        );
    }
}
