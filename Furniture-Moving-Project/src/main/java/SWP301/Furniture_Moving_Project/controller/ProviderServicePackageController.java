package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderPackageSnapshotDTO;
import SWP301.Furniture_Moving_Project.dto.ServicePackageListItemDTO;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers/{providerId}/service-packages")
@CrossOrigin(origins = "*")
public class ProviderServicePackageController {

    private final ProviderPricingService providerPricingService;

    public ProviderServicePackageController(ProviderPricingService providerPricingService) {
        this.providerPricingService = providerPricingService;
    }

    // LEFT LIST: tất cả gói + giá/km (nếu đã cấu hình)
    @GetMapping
    public List<ServicePackageListItemDTO> list(@PathVariable Integer providerId) {
        return providerPricingService.listPackages(providerId);
    }

    // RIGHT DETAIL: snapshot + bảng giá nội thất
    @GetMapping("/{packageId}")
    public ProviderPackageSnapshotDTO detail(@PathVariable Integer providerId,
                                             @PathVariable Integer packageId) {
        return providerPricingService.getPackage(providerId, packageId);
    }

    // Lưu snapshot
    @PutMapping("/{packageId}")
    public void save(@PathVariable Integer providerId,
                     @PathVariable Integer packageId,
                     @RequestBody ProviderPackageSnapshotDTO body) {
        providerPricingService.saveSnapshot(providerId, packageId, body);
    }

    // Xoá toàn bộ snapshot (header + tất cả item)
    @DeleteMapping("/{packageId}")
    public void clear(@PathVariable Integer providerId,
                      @PathVariable Integer packageId) {
        providerPricingService.clearSnapshot(providerId, packageId);
    }

    // Xoá 1 dòng nội thất
    @DeleteMapping("/{packageId}/furniture/{furnitureTypeId}")
    public void deleteItem(@PathVariable Integer providerId,
                           @PathVariable Integer packageId,
                           @PathVariable Integer furnitureTypeId) {
        providerPricingService.deleteItem(providerId, packageId, furnitureTypeId);
    }
}
