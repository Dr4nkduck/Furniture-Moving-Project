package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/provider/dashboard")
public class ProviderDashboardController {

    private final ServiceRequestRepository requestRepo;
    private final ProviderRepository providerRepo;

    public ProviderDashboardController(ServiceRequestRepository requestRepo, ProviderRepository providerRepo) {
        this.requestRepo = requestRepo;
        this.providerRepo = providerRepo;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(Authentication auth) {

        Integer pid = resolveProviderId(auth);
        Map<String, Object> m = new HashMap<>();
        m.put("total",           requestRepo.countByProviderId(pid));
        m.put("assigned",        requestRepo.countByProviderIdAndStatus(pid, "assigned"));
        m.put("in_progress",     requestRepo.countByProviderIdAndStatus(pid, "in_progress"));
        m.put("completed",       requestRepo.countByProviderIdAndStatus(pid, "completed"));
        m.put("cancelled",       requestRepo.countByProviderIdAndStatus(pid, "cancelled"));
        return m;
    }

    private Integer resolveProviderId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String username = auth.getName();
        return providerRepo.findProviderIdByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a provider"));
    }
}