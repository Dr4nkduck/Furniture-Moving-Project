package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.ProviderPackageFurniturePrice;
import SWP301.Furniture_Moving_Project.model.ProviderServicePackage;
import SWP301.Furniture_Moving_Project.repository.ProviderPackageFurniturePriceRepository;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ProviderServicePackageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // <— spring-tx
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/providers/{providerId}/service-packages")
public class ProviderServicePackageController {

    private final ProviderServicePackageRepository providerServicePackageRepo;
    private final ProviderPackageFurniturePriceRepository providerPackageFurnitureRepo;
    private final ProviderRepository providerRepo;

    public ProviderServicePackageController(
            ProviderServicePackageRepository providerServicePackageRepo,
            ProviderPackageFurniturePriceRepository providerPackageFurnitureRepo,
            ProviderRepository providerRepo) {
        this.providerServicePackageRepo = providerServicePackageRepo;
        this.providerPackageFurnitureRepo = providerPackageFurnitureRepo;
        this.providerRepo = providerRepo;
    }

    // Get all service packages for a provider
    @GetMapping
    public List<ProviderServicePackage> getServicePackages(@PathVariable int providerId) {
        return providerServicePackageRepo.findAllByProviderId(providerId);
    }

    // Get service package details
    @GetMapping("/{packageId}")
    public ProviderServicePackage getServicePackage(@PathVariable int providerId, @PathVariable int packageId) {
        return providerServicePackageRepo.findByProviderIdAndPackageId(providerId, packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service Package Not Found"));
    }

    // Create or Update a service package
    @PutMapping("/{packageId}")
    public ProviderServicePackage saveServicePackage(
            @PathVariable int providerId,
            @PathVariable int packageId,
            @RequestBody ProviderServicePackage servicePackage) {

        // ❌ BUG cũ: setId() bị gọi 2 lần → giá trị sau đè trước.
        // ✅ Sửa lại: gán đúng các trường nhận diện (tuỳ mapping của entity bạn)
        // --- Nếu entity có field primitive: providerId, packageId:
        servicePackage.setProviderId(providerId);
        servicePackage.setPackageId(packageId);

        // --- Nếu entity của bạn dùng @ManyToOne (Provider provider; ServicePackage servicePackageRef):
        // servicePackage.setProvider(providerRepo.getReferenceById(providerId));
        // servicePackage.setServicePackageRef(servicePackageRepo.getReferenceById(packageId));

        return providerServicePackageRepo.save(servicePackage);
    }

    // Delete a service package (delete item rows first, then parent)
    @DeleteMapping("/{packageId}")
    @Transactional
    public ResponseEntity<Void> deleteServicePackage(@PathVariable int providerId, @PathVariable int packageId) {
        // 1) Check tồn tại (tránh silent success)
        ProviderServicePackage existing = providerServicePackageRepo
                .findByProviderIdAndPackageId(providerId, packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service Package Not Found"));

        // 2) Xoá các bảng con trước để không dính FK constraint
        providerPackageFurnitureRepo.deleteAllByProviderAndPackage(providerId, packageId);

        // 3) Xoá bản ghi parent (snapshot)
        providerServicePackageRepo.delete(existing);

        // Hoặc nếu bạn muốn xoá theo điều kiện mà không cần load entity:
        // providerServicePackageRepo.deleteByProviderIdAndPackageId(providerId, packageId);

        return ResponseEntity.noContent().build();
    }

    // Add/Update furniture price to a service package
    @PutMapping("/{packageId}/furniture")
    public ProviderPackageFurniturePrice addFurniturePrice(
            @PathVariable int providerId,
            @PathVariable int packageId,
            @RequestBody ProviderPackageFurniturePrice price) {

        price.setProviderId(providerId);
        price.setPackageId(packageId);
        return providerPackageFurnitureRepo.save(price);
    }

    // Delete one furniture price
    @DeleteMapping("/{packageId}/furniture/{furnitureId}")
    public ResponseEntity<Void> deleteFurniturePrice(
            @PathVariable int providerId,
            @PathVariable int packageId,
            @PathVariable int furnitureId) {
        providerPackageFurnitureRepo.deleteOne(providerId, packageId, furnitureId);
        return ResponseEntity.noContent().build();
    }
}
