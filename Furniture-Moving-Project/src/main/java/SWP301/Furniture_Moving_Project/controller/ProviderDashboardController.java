package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/provider/dashboard")
public class ProviderDashboardController {

    private final ServiceRequestRepository requestRepo;

    public ProviderDashboardController(ServiceRequestRepository requestRepo) {
        this.requestRepo = requestRepo;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(
            @RequestParam(required = false) Integer providerId,
            Authentication auth) {

        Integer pid = resolveProviderId(auth, providerId); // tạm lấy query param, bạn có thể map từ auth
        Map<String, Object> m = new HashMap<>();
        m.put("total",           requestRepo.countByProviderId(pid));
        m.put("assigned",        requestRepo.countByProviderIdAndStatus(pid, "assigned"));
        m.put("in_progress",     requestRepo.countByProviderIdAndStatus(pid, "in_progress"));
        m.put("completed",       requestRepo.countByProviderIdAndStatus(pid, "completed"));
        m.put("cancelled",       requestRepo.countByProviderIdAndStatus(pid, "cancelled"));
        return m;
    }

    private Integer resolveProviderId(Authentication auth, Integer providerId) {
        if (providerId != null) return providerId;
        // TODO: map từ user đang đăng nhập → providerId
        throw new IllegalArgumentException("Missing providerId (test nhanh: ?providerId=5)");
    }
}