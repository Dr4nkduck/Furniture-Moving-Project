package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.DTO.CreateServiceRequestDTO;
import SWP301.Furniture_Moving_Project.DTO.CreateFullRequestDTO;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.FullRequestService;
import SWP301.Furniture_Moving_Project.service.ServiceRequestService;

import jakarta.validation.Valid; // <<-- IMPORT QUAN TRỌNG
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final FullRequestService fullRequestService;          // <<-- THÊM FIELD
    private final ServiceRequestRepository serviceRequestRepository;

    public ServiceRequestController(ServiceRequestService serviceRequestService,
                                    FullRequestService fullRequestService,     // <<-- INJECT
                                    ServiceRequestRepository serviceRequestRepository) {
        this.serviceRequestService = serviceRequestService;
        this.fullRequestService = fullRequestService;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    // Tạo request (dùng 2 địa chỉ đã có)
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateServiceRequestDTO dto) {
        Integer id = serviceRequestService.create(dto);
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", Map.of("requestId", id));
        return ResponseEntity.status(201).body(body);
    }

    // Tạo full (địa chỉ + request) trong 1 transaction
    @PostMapping("/full")
    public ResponseEntity<Map<String, Object>> createFull(@Valid @RequestBody CreateFullRequestDTO dto) {
        Integer id = fullRequestService.createAll(dto);
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", Map.of("requestId", id));
        return ResponseEntity.status(201).body(body);
    }

    // (tuỳ chọn) GET /api/requests/{id} để kiểm tra
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Integer id) {
        return serviceRequestRepository.findById(id)
                .map(sr -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("requestId", sr.getRequestId());
                    data.put("customerId", sr.getCustomerId());
                    data.put("providerId", sr.getProviderId());
                    data.put("preferredDate", sr.getPreferredDate());
                    data.put("status", sr.getStatus());
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", true);
                    resp.put("data", data);
                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", false);
                    resp.put("message", "Request not found");
                    return ResponseEntity.status(404).body(resp);
                });
    }
}
