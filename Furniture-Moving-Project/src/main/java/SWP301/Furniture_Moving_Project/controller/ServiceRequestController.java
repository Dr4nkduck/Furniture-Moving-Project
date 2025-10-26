package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.CreateServiceRequestDTO;
import SWP301.Furniture_Moving_Project.dto.CreateFullRequestDTO;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.FullRequestService;
import SWP301.Furniture_Moving_Project.service.ServiceRequestService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final FullRequestService fullRequestService;
    private final ServiceRequestRepository serviceRequestRepository;

    public ServiceRequestController(ServiceRequestService serviceRequestService,
                                    FullRequestService fullRequestService,
                                    ServiceRequestRepository serviceRequestRepository) {
        this.serviceRequestService = serviceRequestService;
        this.fullRequestService = fullRequestService;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    // Helper: build error body thống nhất
    private ResponseEntity<Map<String,Object>> badRequestFrom(BindingResult br) {
        List<Map<String,Object>> errors = br.getFieldErrors().stream()
                .map(fe -> {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("field", fe.getField());           // ví dụ: request.preferredDate, pickupAddress.city, ...
                    m.put("rejected", fe.getRejectedValue());
                    m.put("message", fe.getDefaultMessage());
                    return m;
                })
                .collect(Collectors.toList());
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "Validation failed");
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    // POST /api/requests  (tạo đơn cơ bản)
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateServiceRequestDTO dto,
                                                      BindingResult br) {
        if (br.hasErrors()) {
            return badRequestFrom(br);
        }
        Integer id = serviceRequestService.create(dto);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", Map.of("requestId", id));
        return ResponseEntity.status(201).body(body);
    }

    // POST /api/requests/full  (tạo address + request + items trong 1 transaction)
    @PostMapping("/full")
    public ResponseEntity<Map<String, Object>> createFull(@Valid @RequestBody CreateFullRequestDTO dto,
                                                          BindingResult br) {
        if (br.hasErrors()) {
            return badRequestFrom(br);
        }
        Integer id = fullRequestService.createAll(dto);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", Map.of("requestId", id));
        return ResponseEntity.status(201).body(body);
    }

    // GET /api/requests/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Integer id) {
        return serviceRequestRepository.findById(id)
                .map(sr -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("requestId", sr.getRequestId());
                    data.put("customerId", sr.getCustomerId());
                    data.put("providerId", sr.getProviderId());
                    data.put("preferredDate", sr.getPreferredDate());
                    data.put("status", sr.getStatus());
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("success", true);
                    resp.put("data", data);
                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> {
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("success", false);
                    resp.put("message", "Request not found");
                    return ResponseEntity.status(404).body(resp);
                });
    }
}
