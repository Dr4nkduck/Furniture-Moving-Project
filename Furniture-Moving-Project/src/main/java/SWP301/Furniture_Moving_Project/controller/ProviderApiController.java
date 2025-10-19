package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPricingDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/providers")           // giữ nguyên: /api/providers
@CrossOrigin(origins = "*")
public class ProviderApiController {

    private final ProviderRepository providerRepository;
    private final ProviderPricingService providerPricingService;

    public ProviderApiController(ProviderRepository providerRepository,
                                 ProviderPricingService providerPricingService) {
        this.providerRepository = providerRepository;
        this.providerPricingService = providerPricingService;
    }

    // GET /api/providers
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

    // === PV-002: Pricing ===
    // GET /api/providers/pricing?providerId=123
    @GetMapping("/pricing")
    public ResponseEntity<ProviderPricingDTO> getPricing(
            Authentication auth,
            @RequestParam(name = "providerId", required = false) Integer providerIdFromQuery) {

        Integer providerId = resolveProviderId(auth, providerIdFromQuery);
        ProviderPricingDTO dto = providerPricingService.getPricing(providerId);
        return ResponseEntity.ok(dto);
    }

    // PUT /api/providers/pricing?providerId=123
    @PutMapping("/pricing")
    public ResponseEntity<ProviderPricingDTO> savePricing(
            @Valid @RequestBody ProviderPricingDTO dto,
            Authentication auth,
            @RequestParam(name = "providerId", required = false) Integer providerIdFromQuery) {

        Integer providerId = resolveProviderId(auth, providerIdFromQuery);
        ProviderPricingDTO saved = providerPricingService.upsertPricing(providerId, dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * Tạm thời ưu tiên lấy providerId từ query (?providerId=...)
     * để test nhanh PV-002. Sau khi bạn confirm quan hệ User↔Provider,
     * mình sẽ chuyển sang lấy qua Authentication (username -> Provider).
     */
    private Integer resolveProviderId(Authentication auth, Integer providerIdFromQuery) {
        if (providerIdFromQuery != null) return providerIdFromQuery;

        // TODO: nếu bạn đã có quan hệ User -> Provider, thay phần này:
        // String username = auth.getName();
        // return providerRepository.findBy....(username)
        //         .map(Provider::getProviderId)
        //         .orElseThrow(() -> new IllegalStateException("Provider not found for user"));

        throw new IllegalArgumentException("Missing providerId. Gọi endpoint với ?providerId={id} để test nhanh.");
    }
}
