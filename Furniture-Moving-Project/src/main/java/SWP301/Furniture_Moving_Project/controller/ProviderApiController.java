// src/main/java/SWP301/Furniture_Moving_Project/controller/ProviderApiController.java
package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.FurniturePriceDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderSummaryDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPackageSnapshotDTO;
import SWP301.Furniture_Moving_Project.dto.ServicePackageListItemDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.model.Review;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ReviewRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.ProviderOrderService;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/providers")
public class ProviderApiController {

    private final ProviderRepository providerRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ReviewRepository reviewRepository;
    private final ProviderPricingService providerPricingService;
    private final ProviderOrderService providerOrderService;

    public ProviderApiController(ProviderRepository providerRepository,
                                 ServiceRequestRepository serviceRequestRepository,
                                 ReviewRepository reviewRepository,
                                 ProviderPricingService providerPricingService,
                                 ProviderOrderService providerOrderService) {
        this.providerRepository = providerRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.reviewRepository = reviewRepository;
        this.providerPricingService = providerPricingService;
        this.providerOrderService = providerOrderService;
    }

    // --- Pricing packages snapshot ---
    @GetMapping("/{pid}/service-packages")
    public List<ServicePackageListItemDTO> listPackages(@PathVariable("pid") Integer providerId) {
        return providerPricingService.listPackages(providerId);
    }

    @GetMapping("/{pid}/service-packages/{packageId}")
    public ProviderPackageSnapshotDTO getPackage(@PathVariable("pid") Integer providerId,
                                                 @PathVariable Integer packageId) {
        return providerPricingService.getPackage(providerId, packageId);
    }

    @PutMapping("/{pid}/service-packages/{packageId}")
    public ResponseEntity<?> savePackage(@PathVariable("pid") Integer providerId,
                                         @PathVariable Integer packageId,
                                         @RequestBody SaveSnapshotCommand body) {
        if (body == null) body = new SaveSnapshotCommand();
        ProviderPackageSnapshotDTO dto = new ProviderPackageSnapshotDTO();
        dto.packageNameSnapshot = (body.packageNameSnapshot != null && !body.packageNameSnapshot.isBlank())
                ? body.packageNameSnapshot
                : (body.packageName != null && !body.packageName.isBlank() ? body.packageName : null);
        dto.pricePerKm = body.pricePerKm;
        dto.furniturePrices = (body.furniturePrices != null) ? body.furniturePrices : new ArrayList<>();
        providerPricingService.saveSnapshot(providerId, packageId, dto);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/{pid}/service-packages/{packageId}")
    public ResponseEntity<?> deleteSnapshot(@PathVariable("pid") Integer providerId,
                                            @PathVariable Integer packageId) {
        providerPricingService.clearSnapshot(providerId, packageId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/{pid}/service-packages/{packageId}/items/{furnitureTypeId}")
    public ResponseEntity<?> deleteItem(@PathVariable("pid") Integer providerId,
                                        @PathVariable Integer packageId,
                                        @PathVariable Integer furnitureTypeId) {
        providerPricingService.deleteItem(providerId, packageId, furnitureTypeId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // --- Search providers (for UI) ---
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(value = "name", required = false) String name) {
        List<Provider> entities = (name == null || name.isBlank())
                ? providerRepository.findAll()
                : providerRepository.findByCompanyNameContainingIgnoreCase(name.trim());
        List<Map<String, Object>> data = new ArrayList<>();
        for (Provider p : entities) {
            data.add(Map.of(
                    "providerId", p.getProviderId(),
                    "companyName", p.getCompanyName(),
                    "rating", p.getRating()
            ));
        }
        return Map.of("success", true, "data", data);
    }

    // --- Availability by date ---
    @GetMapping("/availability")
    public Map<String, Object> availability(@RequestParam("date") String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        List<Provider> providers = providerRepository.findAll();
        List<String> busyStatuses = Arrays.asList("assigned", "in_progress");
        List<Map<String, Object>> data = new ArrayList<>();
        for (Provider p : providers) {
            long busyCount = serviceRequestRepository
                    .countByProviderIdAndPreferredDateAndStatusIn(p.getProviderId(), date, busyStatuses);
            data.add(Map.of(
                    "providerId", p.getProviderId(),
                    "companyName", p.getCompanyName(),
                    "rating", p.getRating(),
                    "available", busyCount == 0,
                    "busyCount", busyCount
            ));
        }
        return Map.of("success", true, "data", data);
    }

    // --- Orders (PV-003/004) ---
    @GetMapping("/{providerId}/orders")
    public List<ProviderOrderSummaryDTO> listOrders(@PathVariable Integer providerId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false, name = "q") String q) {
        return providerOrderService.listOrders(providerId, status, q);
    }

    @GetMapping("/{providerId}/orders/{orderId}")
    public ProviderOrderDetailDTO getOrderDetail(@PathVariable Integer providerId,
                                                 @PathVariable Integer orderId) {
        return providerOrderService.getOrderDetail(providerId, orderId);
    }

    @PutMapping("/{providerId}/orders/{orderId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOrderStatus(@PathVariable Integer providerId,
                                  @PathVariable Integer orderId,
                                  @RequestBody SWP301.Furniture_Moving_Project.dto.ProviderOrderUpdateStatusDTO body) {
        providerOrderService.updateOrderStatus(providerId, orderId, body.getStatus(), body.getCancelReason());
    }

    // ----- Command object -----
    public static class SaveSnapshotCommand {
        public String packageName;
        public String packageNameSnapshot;
        public Double pricePerKm;
        public List<FurniturePriceDTO> furniturePrices;
    }
}
