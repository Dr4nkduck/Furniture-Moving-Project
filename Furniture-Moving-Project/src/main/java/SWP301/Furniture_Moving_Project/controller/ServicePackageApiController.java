package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.ServicePackage;
import SWP301.Furniture_Moving_Project.repository.ServicePackageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ServicePackageApiController {

    private final ServicePackageRepository servicePackageRepository;

    public ServicePackageApiController(ServicePackageRepository servicePackageRepository) {
        this.servicePackageRepository = servicePackageRepository;
    }

    @GetMapping("/api/service-packages")
    public ResponseEntity<Map<String, Object>> getActivePackages() {
        List<ServicePackage> packages =
                servicePackageRepository.findByIsActiveTrueOrderByNameAsc();

        // Trả về dạng { success: true, data: [ {code, name}, ... ] }
        List<Map<String, Object>> data = packages.stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("code", p.getCode());
                    m.put("name", p.getName());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", data);
        return ResponseEntity.ok(body);
    }
}
