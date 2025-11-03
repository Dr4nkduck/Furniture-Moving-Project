package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.*;
import SWP301.Furniture_Moving_Project.model.FurnitureType;
import SWP301.Furniture_Moving_Project.repository.FurnitureTypeRepository;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ProviderApiController {

    private final ProviderRepository providerRepo;
    private final ProviderPricingService pricingService;
    private final FurnitureTypeRepository furnitureTypeRepo;

    public ProviderApiController(ProviderRepository providerRepo,
                                 ProviderPricingService pricingService,
                                 FurnitureTypeRepository furnitureTypeRepo) {
        this.providerRepo = providerRepo;
        this.pricingService = pricingService;
        this.furnitureTypeRepo = furnitureTypeRepo;
    }

    /* ---- FIX 404: Các biến thể /me (giữ tương thích JS cũ) ---- */
    @GetMapping({"/providers/me","/provider/me","/me/provider","/auth/me"})
    public Map<String,Object> me(Authentication auth) {
        Integer providerId = null;
        if (auth != null) {
            providerId = providerRepo.findProviderIdByUsername(auth.getName()).orElse(null);
        }
        return Map.of("providerId", providerId);
    }

    /* ---- Packages list của provider ---- */
    @GetMapping("/providers/{providerId}/service-packages")
    public List<PackageOptionDTO> listPackages(@PathVariable Integer providerId) {
        return pricingService.listPackages(providerId);
    }

    /* ---- Chi tiết 1 package: perKm + bảng giá nội thất ---- */
    @GetMapping("/providers/{providerId}/service-packages/{packageId}")
    public PackagePricingDetailDTO packageDetail(@PathVariable Integer providerId,
                                                 @PathVariable Integer packageId) {
        return pricingService.getPackageDetail(providerId, packageId);
    }

    /* ---- Lưu cấu hình giá ---- */
    @PutMapping("/providers/{providerId}/service-packages/{packageId}")
    public ResponseEntity<?> save(@PathVariable Integer providerId,
                                  @PathVariable Integer packageId,
                                  @RequestBody PricingSaveRequestDTO req) {
        req.providerId = providerId;
        req.packageId  = packageId;
        pricingService.savePackagePricing(req);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /* ---- Catalog nội thất (để load lên bảng) ---- */
    @GetMapping("/furniture-items")
    public List<Map<String,Object>> furnitureItems() {
        List<Map<String,Object>> out = new ArrayList<>();
        for (FurnitureType ft : furnitureTypeRepo.findAll()) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("furnitureItemId", ft.getFurnitureTypeId());
            m.put("furnitureItemName", ft.getName());
            out.add(m);
        }
        return out;
    }
}
