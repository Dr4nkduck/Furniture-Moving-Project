package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.DTO.ProviderDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPricingDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPackagePricingDTO;
import SWP301.Furniture_Moving_Project.dto.FurniturePriceDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import SWP301.Furniture_Moving_Project.service.ProviderCatalogService;
// import SWP301.Furniture_Moving_Project.service.ProviderOrderService; // <- Uncomment if you already added PV-003

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

    // PV-002 (new package + furniture catalog)
    private final ProviderCatalogService providerCatalogService;

    // PV-003/004/005 (orders) – inject when you’re ready
    // private final ProviderOrderService providerOrderService;

    public ProviderApiController(ProviderRepository providerRepository,
                                 ProviderPricingService providerPricingService,
                                 ProviderCatalogService providerCatalogService
                                 // , ProviderOrderService providerOrderService
    ) {
        this.providerRepository = providerRepository;
        this.providerPricingService = providerPricingService;
        this.providerCatalogService = providerCatalogService;
        // this.providerOrderService = providerOrderService;
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

    // -------------------------------------------------------------------------
    // PV-002 (A) – Your existing GLOBAL provider pricing (keep as default)
    // Paths are FIXED to be relative to /api/providers
    // -------------------------------------------------------------------------

    // GET /api/providers/pricing
    @GetMapping("/pricing")
    public ResponseEntity<ProviderPricingDTO> getGlobalPricing(Authentication auth,
                                                               @RequestParam(required = false) Integer providerId) {
        Integer pid = resolveProviderId(auth, providerId);
        return ResponseEntity.ok(providerPricingService.getPricing(pid));
    }

    // PUT /api/providers/pricing
    @PutMapping("/pricing")
    public ResponseEntity<ProviderPricingDTO> upsertGlobalPricing(@Valid @RequestBody ProviderPricingDTO dto,
                                                                  Authentication auth,
                                                                  @RequestParam(required = false) Integer providerId) {
        Integer pid = resolveProviderId(auth, providerId);
        return ResponseEntity.ok(providerPricingService.upsertPricing(pid, dto));
    }

    // -------------------------------------------------------------------------
    // PV-002 (B) – NEW: Package dropdown + per-package route fees + furniture prices
    // These do NOT replace your global pricing; they add per-package detail.
    // -------------------------------------------------------------------------

    // GET /api/providers/packages  -> dropdown data (id, code, name)
    @GetMapping("/packages")
    public ResponseEntity<List<Map<String, Object>>> listPackages() {
        var list = providerCatalogService.listActivePackages();
        var out = new ArrayList<Map<String, Object>>();
        for (var sp : list) {
            var m = new HashMap<String, Object>();
            m.put("packageId", sp.getPackageId());
            m.put("code", sp.getCode());
            m.put("name", sp.getName());
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    // GET /api/providers/packages/pricing?packageId=...&providerId=...
    @GetMapping("/packages/pricing")
    public ResponseEntity<ProviderPackagePricingDTO> getPackagePricing(Authentication auth,
                                                                       @RequestParam Integer packageId,
                                                                       @RequestParam(required = false) Integer providerId) {
        Integer pid = resolveProviderId(auth, providerId);
        return ResponseEntity.ok(providerCatalogService.getPackagePricing(pid, packageId));
    }

    // PUT /api/providers/packages/pricing?providerId=...
    @PutMapping("/packages/pricing")
    public ResponseEntity<ProviderPackagePricingDTO> upsertPackagePricing(Authentication auth,
                                                                          @Valid @RequestBody ProviderPackagePricingDTO dto,
                                                                          @RequestParam(required = false) Integer providerId) {
        Integer pid = resolveProviderId(auth, providerId);
        return ResponseEntity.ok(providerCatalogService.upsertPackagePricing(pid, dto));
    }

    // GET /api/providers/furniture-prices?providerId=...
    @GetMapping("/furniture-prices")
    public ResponseEntity<List<FurniturePriceDTO>> getFurniturePrices(Authentication auth,
                                                                      @RequestParam(required = false) Integer providerId) {
        Integer pid = resolveProviderId(auth, providerId);
        return ResponseEntity.ok(providerCatalogService.listFurniturePrices(pid));
    }

    // PUT /api/providers/furniture-prices?providerId=...
    @PutMapping("/furniture-prices")
    public ResponseEntity<List<FurniturePriceDTO>> upsertFurniturePrices(Authentication auth,
                                                                         @RequestBody List<FurniturePriceDTO> items,
                                                                         @RequestParam(required = false) Integer providerId) {
        Integer pid = resolveProviderId(auth, providerId);
        return ResponseEntity.ok(providerCatalogService.upsertFurniturePrices(pid, items));
    }

    // -------------------------------------------------------------------------
    // PV-003/004/005 (optional – uncomment when service is ready)
    // -------------------------------------------------------------------------

    // // GET /api/providers/orders?status=assigned&status=in_progress&providerId=...
    // @GetMapping("/orders")
    // public ResponseEntity<List<ProviderOrderListItemDTO>> listOrders(
    //         Authentication auth,
    //         @RequestParam(required = false) Integer providerId,
    //         @RequestParam(required = false, name = "status") List<String> statuses) {
    //     Integer pid = resolveProviderId(auth, providerId);
    //     return ResponseEntity.ok(providerOrderService.listOrders(pid, statuses));
    // }

    // -------------------------------------------------------------------------
    // Helper to resolve providerId (temporary)
    // Later map from Authentication → providerId in your own way.
    // -------------------------------------------------------------------------
    private Integer resolveProviderId(Authentication auth, Integer providerId) {
        if (providerId != null) return providerId;
        // TODO: Map from auth.getName() to your User -> Provider (e.g., via repository)
        // Throw for now so you can test with ?providerId=
        throw new IllegalArgumentException("Missing providerId (for quick testing pass ?providerId=...)");
    }
}
