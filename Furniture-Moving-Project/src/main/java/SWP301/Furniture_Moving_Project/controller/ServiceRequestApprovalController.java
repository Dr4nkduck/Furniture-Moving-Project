package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestApprovalController {

    private final ServiceRequestRepository serviceRequestRepo;
    private final ProviderRepository providerRepo;

    public ServiceRequestApprovalController(ServiceRequestRepository serviceRequestRepo,
                                            ProviderRepository providerRepo) {
        this.serviceRequestRepo = serviceRequestRepo;
        this.providerRepo = providerRepo;
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<?> approve(@PathVariable Integer id, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        int rows;
        if (isAdmin) {
            rows = serviceRequestRepo.approveIfPending(id); // ADMIN: duyệt mọi đơn pending
        } else {
            Integer providerId = providerRepo.findProviderIdByUsername(auth.getName()).orElse(null);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Tài khoản không phải PROVIDER"));
            }
            rows = serviceRequestRepo.approveIfPendingByProvider(id, providerId); // PROVIDER: chỉ đơn của mình
        }

        if (rows == 1) return ResponseEntity.ok(Map.of("id", id, "status", "completed"));
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Không có quyền hoặc trạng thái không hợp lệ", "id", id));
    }
}
