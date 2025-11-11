package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPricingDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPackagePricingDTO;
import SWP301.Furniture_Moving_Project.dto.FurniturePriceDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.dto.FurniturePriceDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPackageSnapshotDTO;
import SWP301.Furniture_Moving_Project.dto.ServicePackageListItemDTO;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import SWP301.Furniture_Moving_Project.dto.ProviderOrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderSummaryDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderUpdateStatusDTO;
import SWP301.Furniture_Moving_Project.service.ProviderOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import java.util.*;


/**
 * Provider API — đã bổ sung các endpoint PV-002 (Service Package Pricing Snapshot).
 * Giữ nguyên các API cũ của bạn ở bên dưới nếu có.
 */
@RestController
@RequestMapping("/api/providers")
public class ProviderApiController {

    private final ProviderPricingService providerPricingService;
    private final ProviderRepository providerRepository;

    public ProviderApiController(ProviderPricingService providerPricingService, ProviderRepository providerRepository) {
        this.providerPricingService = providerPricingService;
        this.providerRepository = providerRepository;
    }

    // ============================================================
    // GET /api/providers/available
    // Returns list of available providers for request form
    // Trả: [{ providerId, companyName, rating }]
    // ============================================================
    // === THÊM MỚI (phiên bản nhẹ, tránh lỗi entity mismatch) ===
    @GetMapping("/available")
    public List<Map<String,Object>> getAvailableProvidersLight() {
        return providerRepository.findAvailableProvidersLight();
    }

    // ============================================================
    // PV-002: LIST packages (left panel)
    // GET /api/providers/{pid}/service-packages
    // Trả: [{ packageId, packageName, basePackageName, pricePerKm }]
    // ============================================================
    @GetMapping("/{pid}/service-packages")
    public List<ServicePackageListItemDTO> listPackages(@PathVariable("pid") Integer providerId) {
        return providerPricingService.listPackages(providerId);
    }

    // ============================================================
    // PV-002: GET one package snapshot (right panel)
    // GET /api/providers/{pid}/service-packages/{packageId}
    // Trả: { packageNameSnapshot, pricePerKm, furniturePrices: [...] }
    // ============================================================
    @GetMapping("/{pid}/service-packages/{packageId}")
    public ProviderPackageSnapshotDTO getPackage(@PathVariable("pid") Integer providerId,
                                                 @PathVariable Integer packageId) {
        return providerPricingService.getPackage(providerId, packageId);
    }

    // ============================================================
    // PV-002: PUT save snapshot (create/update)
    // PUT /api/providers/{pid}/service-packages/{packageId}
    //
    // Front-end gửi "packageName" (alias) hoặc "packageNameSnapshot".
    // Controller map sang DTO chuẩn trước khi gọi service.
    //
    // Body mẫu:
    // {
    //   "packageName": "Gói Nhà Phố (snapshot)",   // hoặc dùng packageNameSnapshot
    //   "pricePerKm": 120000,
    //   "furniturePrices": [
    //     {"furnitureItemId": 3, "price": 500000},
    //     {"furnitureItemName":"Sofa 3 chỗ", "price": 1000000}, // tên mới -> tạo FurnitureType mới
    //     {"furnitureItemId": 8, "price": null} // null => xoá giá item này
    //   ]
    // }
    // ============================================================
    @PutMapping("/{pid}/service-packages/{packageId}")
    public ResponseEntity<?> savePackage(@PathVariable("pid") Integer providerId,
                                         @PathVariable Integer packageId,
                                         @RequestBody SaveSnapshotCommand body) {
        if (body == null) body = new SaveSnapshotCommand();
        ProviderPackageSnapshotDTO dto = new ProviderPackageSnapshotDTO();
        // alias: ưu tiên packageNameSnapshot nếu có, ngược lại dùng packageName
        dto.packageNameSnapshot = (body.packageNameSnapshot != null && !body.packageNameSnapshot.isBlank())
                ? body.packageNameSnapshot
                : (body.packageName != null && !body.packageName.isBlank() ? body.packageName : null);
        dto.pricePerKm = body.pricePerKm;
        dto.furniturePrices = (body.furniturePrices != null) ? body.furniturePrices : new ArrayList<>();

        providerPricingService.saveSnapshot(providerId, packageId, dto);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ============================================================
    // PV-002: DELETE snapshot (xoá toàn bộ cấu hình gói)
    // DELETE /api/providers/{pid}/service-packages/{packageId}
    // ============================================================
    @DeleteMapping("/{pid}/service-packages/{packageId}")
    public ResponseEntity<?> deleteSnapshot(@PathVariable("pid") Integer providerId,
                                            @PathVariable Integer packageId) {
        providerPricingService.clearSnapshot(providerId, packageId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ============================================================
    // PV-002: DELETE one furniture item price in snapshot
    // DELETE /api/providers/{pid}/service-packages/{packageId}/items/{furnitureTypeId}
    // ============================================================
    @DeleteMapping("/{pid}/service-packages/{packageId}/items/{furnitureTypeId}")
    public ResponseEntity<?> deleteItem(@PathVariable("pid") Integer providerId,
                                        @PathVariable Integer packageId,
                                        @PathVariable Integer furnitureTypeId) {
        providerPricingService.deleteItem(providerId, packageId, furnitureTypeId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ============================================================
    // (TÙY CHỌN) Nếu bạn muốn hỗ trợ JS cũ gọi /api/providers/me:
    // Bật endpoint này để trả về providerId và companyName cho UI (không bắt buộc,
    // vì hiện tại UI đã lấy providerId từ <meta>).
    // ============================================================
    // @GetMapping("/me")
    // public Map<String,Object> me(Principal principal) {
    //     Integer providerId = ...; // map từ principal
    //     String companyName = ...; // lấy từ Provider
    //     return Map.of("providerId", providerId, "companyName", companyName);
    // }

    // ============================================================
    // YOUR OTHER ENDPOINTS (giữ nguyên các API khác của bạn) ...
    // ============================================================

    // ---------- Inner class: Command object nhận body PUT ----------
    public static class SaveSnapshotCommand {
        public String packageName;              // alias từ FE
        public String packageNameSnapshot;      // tên chuẩn trong BE
        public Double pricePerKm;
        public List<FurniturePriceDTO> furniturePrices;
    }

    // === THÊM FIELD (không phá constructor sẵn có) ===
    @Autowired
    private ProviderOrderService providerOrderService;

    // === PV-003: List/Search/Filter orders của provider ===
    // Hỗ trợ cả 2 dạng: (a) class-level không có base path  (b) class-level có @RequestMapping("/api/providers")
    @GetMapping(path = {"/api/providers/{providerId}/orders", "/{providerId}/orders"})
    public List<ProviderOrderSummaryDTO> listOrders(
            @PathVariable Integer providerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "q") String q) {
        return providerOrderService.listOrders(providerId, status, q);
    }

    // === PV-003: Xem chi tiết đơn ===
    @GetMapping(path = {"/api/providers/{providerId}/orders/{orderId}", "/{providerId}/orders/{orderId}"})
    public ProviderOrderDetailDTO getOrderDetail(@PathVariable Integer providerId,
                                                 @PathVariable Integer orderId) {
        return providerOrderService.getOrderDetail(providerId, orderId);
    }

    // === PV-004: Cập nhật trạng thái đơn ===
    // status: pending/accepted/declined/in_progress/completed/cancelled
    @PutMapping(path = {"/api/providers/{providerId}/orders/{orderId}/status", "/{providerId}/orders/{orderId}/status"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOrderStatus(@PathVariable Integer providerId,
                                  @PathVariable Integer orderId,
                                  @RequestBody ProviderOrderUpdateStatusDTO body) {
        providerOrderService.updateOrderStatus(providerId, orderId, body.getStatus(), body.getCancelReason());
    }
}
