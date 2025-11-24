package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.FurniturePriceDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderSummaryDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderUpdateStatusDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderServicePackageDetailDto;
import SWP301.Furniture_Moving_Project.dto.ProviderServicePackageSummaryDto;
import SWP301.Furniture_Moving_Project.dto.SaveProviderPackageRequest;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.ProviderOrderService;
import jakarta.transaction.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;

@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProviderApiController {

    private final ProviderRepository providerRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ProviderOrderService providerOrderService;
    private final JdbcTemplate jdbc;   // dùng cho PV-002 (service-packages)

    public ProviderApiController(ProviderRepository providerRepository,
                                 ServiceRequestRepository serviceRequestRepository,
                                 ProviderOrderService providerOrderService,
                                 JdbcTemplate jdbc) {
        this.providerRepository = providerRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.providerOrderService = providerOrderService;
        this.jdbc = jdbc;
    }

    /** (Tuỳ chọn) Endpoint nhẹ cho dropdown: /api/providers/available */
    @GetMapping("/available")
    public List<Map<String, Object>> getAvailableProvidersLight() {
        return providerRepository.findAvailableProvidersLight();
    }

    @GetMapping
    @Transactional
    public Map<String, Object> list(@RequestParam(required = false) String status) {
        List<Map<String, Object>> data;

        if (status == null || status.isBlank()) {
            data = providerRepository.findAvailableProvidersLight();
        } else {
            data = providerRepository
                    .findByVerificationStatusOrderByCompanyNameAsc(status)
                    .stream()
                    .map(p -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("providerId", p.getProviderId());
                        m.put("companyName", p.getCompanyName());
                        m.put("rating", p.getRating());
                        return m;
                    })
                    .collect(Collectors.toList());
        }

        return Map.of("success", true, "data", data);
    }

    // SEARCH: GET /api/providers/search?name=...
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam(required = false) String name) {
        var list = (name == null || name.isBlank())
                ? providerRepository.findAll()
                : providerRepository.findByCompanyNameContainingIgnoreCase(name.trim());

        var data = list.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("providerId", p.getProviderId());
            m.put("companyName", p.getCompanyName());
            m.put("rating", p.getRating());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // AVAILABILITY: GET /api/providers/availability?date=YYYY-MM-DD
    @GetMapping("/availability")
    public ResponseEntity<Map<String, Object>> availability(@RequestParam("date") String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        List<String> busyStatuses = Arrays.asList("assigned", "in_progress");

        var data = new ArrayList<Map<String, Object>>();
        for (Provider p : providerRepository.findAll()) {
            long busyCount = serviceRequestRepository
                    .countByProviderIdAndPreferredDateAndStatusIn(
                            p.getProviderId(), date, busyStatuses);

            Map<String, Object> m = new HashMap<>();
            m.put("providerId", p.getProviderId());
            m.put("companyName", p.getCompanyName());
            m.put("rating", p.getRating());
            m.put("available", busyCount == 0);
            m.put("busyCount", busyCount);
            data.add(m);
        }

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // PV-003: List/Search orders của provider
    // GET /api/providers/{providerId}/orders?status=...&q=...
    @GetMapping("/{providerId}/orders")
    public List<ProviderOrderSummaryDTO> listOrders(@PathVariable Integer providerId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false, name = "q") String q) {
        return providerOrderService.listOrders(providerId, status, q);
    }

    // GET /api/providers/{providerId}/orders/{orderId}
    @GetMapping("/{providerId}/orders/{orderId}")
    public ProviderOrderDetailDTO getOrderDetail(@PathVariable Integer providerId,
                                                 @PathVariable Integer orderId) {
        return providerOrderService.getOrderDetail(providerId, orderId);
    }

    // PV-004: Cập nhật trạng thái đơn
    @PutMapping("/{providerId}/orders/{orderId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void updateOrderStatus(@PathVariable Integer providerId,
                                  @PathVariable Integer orderId,
                                  @RequestBody ProviderOrderUpdateStatusDTO body) {

        providerOrderService.updateOrderStatus(providerId, orderId, body.getStatus(), body.getCancelReason());

        serviceRequestRepository.findById(orderId).ifPresent(sr -> {
            String newStatus = body.getStatus();
            if (newStatus != null) {
                newStatus = newStatus.trim();
            }
            sr.setStatus(newStatus);
            serviceRequestRepository.save(sr);
        });
    }

    // === Nút "Xác nhận đã thanh toán" cho Provider (nếu còn dùng) ===
    @PostMapping("/{providerId}/orders/{orderId}/confirm-payment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPayment(@PathVariable Integer providerId,
                               @PathVariable Integer orderId) {
        providerOrderService.confirmPayment(providerId, orderId);
    }

    // GET /api/providers/me - Lấy thông tin provider hiện tại
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalArgumentException("Chưa đăng nhập");
        }
        String username = auth.getName();
        Integer providerId = providerRepository
                .findProviderIdByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy provider cho user: " + username));

        return Map.of(
                "success", true,
                "providerId", providerId
        );
    }

    // ============================================================
    // PV-002 • Service packages (bảng giá & snapshot)
    // ============================================================

    // 1) LIST: GET /api/providers/{providerId}/service-packages
    @GetMapping("/{providerId}/service-packages")
    public List<ProviderServicePackageSummaryDto> listServicePackages(@PathVariable int providerId) {
        String sql = """
            SELECT sp.package_id,
                   sp.name AS package_name,
                   sp.name AS base_package_name,
                   psp.per_km AS price_per_km,
                   psp.package_name_snapshot
            FROM service_packages sp
            LEFT JOIN provider_service_packages psp
                   ON psp.package_id = sp.package_id
                  AND psp.provider_id = ?
            WHERE sp.is_active = 1
            ORDER BY sp.package_id
            """;

        return jdbc.query(sql, (rs, rowNum) -> {
            ProviderServicePackageSummaryDto dto = new ProviderServicePackageSummaryDto();
            dto.setPackageId(rs.getInt("package_id"));
            dto.setPackageName(rs.getString("package_name"));
            dto.setBasePackageName(rs.getString("base_package_name"));
            dto.setPricePerKm(rs.getBigDecimal("price_per_km"));
            dto.setPackageNameSnapshot(rs.getString("package_name_snapshot"));
            return dto;
        }, providerId);
    }

    // 2) DETAIL: GET /api/providers/{providerId}/service-packages/{packageId}
    @GetMapping("/{providerId}/service-packages/{packageId}")
    public ProviderServicePackageDetailDto getServicePackageDetail(@PathVariable int providerId,
                                                                   @PathVariable int packageId) {
        ProviderServicePackageDetailDto dto = new ProviderServicePackageDetailDto();
        dto.setProviderId(providerId);
        dto.setPackageId(packageId);

        String sqlHeader = """
            SELECT TOP 1 per_km, package_name_snapshot
            FROM provider_service_packages
            WHERE provider_id = ? AND package_id = ?
            """;

        List<Map<String, Object>> headerRows = jdbc.queryForList(sqlHeader, providerId, packageId);
        if (!headerRows.isEmpty()) {
            Map<String, Object> row = headerRows.get(0);
            dto.setPricePerKm((BigDecimal) row.get("per_km"));
            dto.setPackageNameSnapshot((String) row.get("package_name_snapshot"));
        } else {
            dto.setPricePerKm(null);
            dto.setPackageNameSnapshot(null);
        }

        String sqlFurniture = """
            SELECT ppfp.id           AS furniture_item_id,
                   ft.name           AS furniture_item_name,
                   ppfp.price        AS price
            FROM provider_package_furniture_prices ppfp
            JOIN furniture_types ft
              ON ft.furniture_type_id = ppfp.furniture_type_id
            WHERE ppfp.provider_id = ? AND ppfp.package_id = ?
            ORDER BY ft.name
            """;

        List<FurniturePriceDTO> items = jdbc.query(sqlFurniture, (rs, rowNum) -> {
            FurniturePriceDTO f = new FurniturePriceDTO();
            f.setFurnitureItemId(rs.getInt("furniture_item_id"));
            f.setFurnitureItemName(rs.getString("furniture_item_name"));
            f.setPrice(rs.getBigDecimal("price"));
            return f;
        }, providerId, packageId);

        dto.setFurniturePrices(items);
        return dto;
    }

    // 3) SAVE: PUT /api/providers/{providerId}/service-packages/{packageId}
    @PutMapping("/{providerId}/service-packages/{packageId}")
    @Transactional
    public ResponseEntity<?> saveServicePackageConfig(@PathVariable int providerId,
                                                      @PathVariable int packageId,
                                                      @RequestBody SaveProviderPackageRequest body) {

        if (body.getProviderId() != null && !body.getProviderId().equals(providerId)) {
            return ResponseEntity.badRequest().body("providerId path/body không khớp");
        }
        if (body.getPackageId() != null && !body.getPackageId().equals(packageId)) {
            return ResponseEntity.badRequest().body("packageId path/body không khớp");
        }

        String snapshot = emptyToNull(body.getPackageNameSnapshot());
        BigDecimal perKm = body.getPricePerKm();
        List<FurniturePriceDTO> furniturePrices = body.getFurniturePrices() != null
                ? body.getFurniturePrices()
                : new ArrayList<>();

        boolean isDelete = perKm == null && furniturePrices.isEmpty() && snapshot == null;
        if (isDelete) {
            jdbc.update("DELETE FROM provider_package_furniture_prices WHERE provider_id=? AND package_id=?",
                    providerId, packageId);
            jdbc.update("DELETE FROM provider_service_packages WHERE provider_id=? AND package_id=?",
                    providerId, packageId);
            return ResponseEntity.ok().build();
        }

        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM provider_service_packages WHERE provider_id=? AND package_id=?",
                Integer.class, providerId, packageId
        );
        if (existing != null && existing > 0) {
            jdbc.update("""
                        UPDATE provider_service_packages
                        SET per_km = ?, package_name_snapshot = ?
                        WHERE provider_id = ? AND package_id = ?
                        """,
                    perKm, snapshot, providerId, packageId);
        } else {
            jdbc.update("""
                        INSERT INTO provider_service_packages(provider_id, package_id, per_km, package_name_snapshot)
                        VALUES (?,?,?,?)
                        """,
                    providerId, packageId, perKm, snapshot);
        }

        jdbc.update("DELETE FROM provider_package_furniture_prices WHERE provider_id=? AND package_id=?",
                providerId, packageId);

        for (FurniturePriceDTO fp : furniturePrices) {
            if (fp.getPrice() == null) continue;

            Integer typeId = fp.getFurnitureItemId();
            String name = fp.getFurnitureItemName();

            if (typeId == null) {
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                typeId = findOrCreateFurnitureTypeIdByName(name.trim());
            }

            if (typeId != null) {
                jdbc.update("""
                        INSERT INTO provider_package_furniture_prices(provider_id, package_id, furniture_type_id, price)
                        VALUES (?,?,?,?)
                        """, providerId, packageId, typeId, fp.getPrice());
            }
        }

        return ResponseEntity.ok().build();
    }

    // ================== helper cho PV-002 ==================
    private String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Integer findOrCreateFurnitureTypeIdByName(String name) {
        Integer id = null;
        try {
            id = jdbc.queryForObject(
                    "SELECT furniture_type_id FROM furniture_types WHERE LOWER(name) = LOWER(?)",
                    Integer.class, name);
        } catch (EmptyResultDataAccessException ignore) {
        }

        if (id != null) {
            return id;
        }

        String code = generateCodeFromName(name);
        jdbc.update("""
                    INSERT INTO furniture_types(code, name, unit)
                    VALUES (?,?,N'chiếc')
                    """, code, name);

        return jdbc.queryForObject(
                "SELECT furniture_type_id FROM furniture_types WHERE LOWER(name) = LOWER(?)",
                Integer.class, name);
    }

    private String generateCodeFromName(String name) {
        String raw = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        if (raw.length() > 40) raw = raw.substring(0, 40);
        if (raw.isEmpty()) raw = "ITEM";
        return raw;
    }
}
