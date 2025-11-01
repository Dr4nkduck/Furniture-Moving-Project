package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderPackagePricingDTO;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers/pricing")
@CrossOrigin(origins = "*")
public class ProviderPricingApiController {

    private final ProviderPricingService pricingService;

    public ProviderPricingApiController(ProviderPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/{providerId}/packages")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public List<ProviderPackagePricingDTO> listPackages(@PathVariable Integer providerId) {
        return pricingService.listPackagesForProvider(providerId);
    }

    @GetMapping("/{providerId}/packages/{packageId}")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public ProviderPackagePricingDTO getPackage(@PathVariable Integer providerId,
                                                @PathVariable Integer packageId) {
        return pricingService.getPackagePricing(providerId, packageId);
    }

    @PutMapping("/{providerId}/packages/{packageId}")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> savePackage(@PathVariable Integer providerId,
                                         @PathVariable Integer packageId,
                                         @RequestBody ProviderPackagePricingDTO body) {
        body.setProviderId(providerId);
        body.setPackageId(packageId);
        pricingService.savePackagePricing(body);
        return ResponseEntity.ok().build();
    }
}
