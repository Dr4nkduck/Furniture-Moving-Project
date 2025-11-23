package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.AiQuoteSettings;
import SWP301.Furniture_Moving_Project.repository.AiQuoteSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiQuoteSettingsController {

    private final AiQuoteSettingsRepository settingsRepository;

    public AiQuoteSettingsController(AiQuoteSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        AiQuoteSettings s = settingsRepository.findTopByOrderByIdAsc();
        if (s == null) {
            // fallback nếu chưa seed
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("currency", "VND");
            fallback.put("pricePerKm", 10000);
            fallback.put("minFare", 50000);
            fallback.put("maxDaysAhead", 30);
            return ResponseEntity.ok(fallback);
        }

        Map<String, Object> dto = new HashMap<>();
        dto.put("currency", s.getCurrency());
        dto.put("pricePerKm", s.getPricePerKm());
        dto.put("minFare", s.getMinFare());
        dto.put("maxDaysAhead", s.getMaxDaysAhead());
        return ResponseEntity.ok(dto);
    }
}
