package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.CustomerTrendsResponseDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderStatsResponseDTO;
import SWP301.Furniture_Moving_Project.service.AdminAnalyticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService svc;
    public AdminAnalyticsController(AdminAnalyticsService svc) { this.svc = svc; }

    // ----- UC-A03: Provider Stats -----
    @GetMapping("/providers")
    public ProviderStatsResponseDTO providerStats(
            @RequestParam String from, @RequestParam String to) {
        return svc.providerStats(from, to);
    }

    @GetMapping(value = "/providers.csv", produces = "text/csv")
    public ResponseEntity<String> providerStatsCsv(
            @RequestParam String from, @RequestParam String to) {
        String csv = svc.providerStatsCsv(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=provider_stats.csv")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    // ----- UC-A04: Customer Trends -----
    @GetMapping("/customers")
    public CustomerTrendsResponseDTO customerTrends(
            @RequestParam String from, @RequestParam String to,
            @RequestParam(defaultValue = "10") int top) {
        return svc.customerTrends(from, to, top);
    }

    @GetMapping(value = "/customers.csv", produces = "text/csv")
    public ResponseEntity<String> customerTrendsCsv(
            @RequestParam String from, @RequestParam String to,
            @RequestParam(defaultValue = "10") int top) {
        String csv = svc.customerTrendsCsv(from, to, top);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customer_trends.csv")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }
}
