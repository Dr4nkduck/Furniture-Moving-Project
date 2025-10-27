package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.CustomerTrendsDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderStatsDTO;
import SWP301.Furniture_Moving_Project.service.AdminAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService service;
    public AdminAnalyticsController(AdminAnalyticsService service) { this.service = service; }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid range: 'from' date must be on or before 'to' date.");
        }
    }

    // ---------- UC-A03 ----------
    @GetMapping("/provider-stats")
    public List<ProviderStatsDTO> providerStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, name = "q") String nameFilter) {
        validateRange(from, to);
        return service.getProviderStats(from, to, nameFilter);
    }

    @GetMapping(value="/provider-stats.csv", produces="text/csv")
    public ResponseEntity<byte[]> providerStatsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, name = "q") String nameFilter) {
        validateRange(from, to);
        byte[] body = service.toProviderStatsCsv(service.getProviderStats(from, to, nameFilter))
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=provider-stats.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    // ---------- UC-A04 ----------
    @GetMapping("/customer-trends")
    public CustomerTrendsDTO customerTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(defaultValue = "month") String granularity) {
        validateRange(from, to);
        return service.getCustomerTrends(from, to, top, granularity);
    }

    @GetMapping(value="/customer-trends/funnel.csv", produces="text/csv")
    public ResponseEntity<byte[]> funnelCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "month") String granularity) {
        validateRange(from, to);
        CustomerTrendsDTO dto = service.getCustomerTrends(from, to, 10, granularity);
        byte[] body = service.toFunnelCsv(dto.getFunnel()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=funnel.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    @GetMapping(value="/customer-trends/corridors.csv", produces="text/csv")
    public ResponseEntity<byte[]> corridorsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int top) {
        validateRange(from, to);
        CustomerTrendsDTO dto = service.getCustomerTrends(from, to, top, "month");
        byte[] body = service.toCorridorsCsv(dto.getTopCorridors()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=top-corridors.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    @GetMapping(value="/customer-trends/cancel-reasons.csv", produces="text/csv")
    public ResponseEntity<byte[]> cancelReasonsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to);
        CustomerTrendsDTO dto = service.getCustomerTrends(from, to, 10, "month");
        byte[] body = service.toCancelReasonsCsv(dto.getCancelReasons()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cancel-reasons.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
