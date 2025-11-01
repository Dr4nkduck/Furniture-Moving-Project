package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;

import SWP301.Furniture_Moving_Project.dto.ProviderPackagePricingDTO;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;




import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProviderApiController {

    private final ProviderRepository providerRepository;

    // PV-002 (existing global pricing)
    private final ProviderPricingService providerPricingService;



    // PV-003/004/005 (orders) – inject when you’re ready
    // private final ProviderOrderService providerOrderService;

    public ProviderApiController(ProviderRepository providerRepository,
                                 ProviderPricingService providerPricingService

                                 // , ProviderOrderService providerOrderService
    ) {
        this.providerRepository = providerRepository;
        this.providerPricingService = providerPricingService;


    }

    // -------------------------------------------------------------------------
    // BASIC LIST
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<Provider> entities = providerRepository.findAll();

        List<ProviderDTO> data = entities.stream()
                .map(p -> new ProviderDTO(
                        p.getProviderId(),
                        p.getCompanyName(),
                        p.getRating()
                ))
                .collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }


    // ========== PV-002: Pricing APIs ==========
    @GetMapping("/{providerId}/pricing/packages")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public java.util.List<ProviderPackagePricingDTO> listPackages(@PathVariable Integer providerId) {
        return providerPricingService.listPackagesForProvider(providerId);
    }

    @GetMapping("/{providerId}/pricing/packages/{packageId}")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public ProviderPackagePricingDTO getPackage(@PathVariable Integer providerId,
                                                @PathVariable Integer packageId) {
        return providerPricingService.getPackagePricing(providerId, packageId);
    }

    @PutMapping("/{providerId}/pricing/packages/{packageId}")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> savePackage(@PathVariable Integer providerId,
                                         @PathVariable Integer packageId,
                                         @RequestBody ProviderPackagePricingDTO body) {
        body.setProviderId(providerId);
        body.setPackageId(packageId);
        providerPricingService.savePackagePricing(body);
        return ResponseEntity.ok().build();
    }

}
