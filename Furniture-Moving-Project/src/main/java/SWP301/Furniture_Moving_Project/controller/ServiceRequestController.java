package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.CreateFullRequestDTO;
import SWP301.Furniture_Moving_Project.dto.CreateServiceRequestDTO;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.FullRequestService;
import SWP301.Furniture_Moving_Project.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final FullRequestService fullRequestService;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ProviderRepository providerRepository;
    private final JdbcTemplate jdbc;

    public ServiceRequestController(ServiceRequestService serviceRequestService,
                                    FullRequestService fullRequestService,
                                    ServiceRequestRepository serviceRequestRepository,
                                    ProviderRepository providerRepository,
                                    JdbcTemplate jdbc) {
        this.serviceRequestService = serviceRequestService;
        this.fullRequestService = fullRequestService;
        this.serviceRequestRepository = serviceRequestRepository;
        this.providerRepository = providerRepository;
        this.jdbc = jdbc;
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
        
        // Automatically get customerId from authenticated user (more secure)
        Integer customerId = getCurrentCustomerId();
        if (customerId == null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", "User must be logged in as a customer to create requests");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
        
        // Override customerId in DTO with the authenticated user's customerId
        if (dto.getRequest() != null) {
            dto.getRequest().setCustomerId(customerId);
        }
        
        Integer id = fullRequestService.createAll(dto);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", Map.of("requestId", id));
        return ResponseEntity.status(201).body(body);
    }
    
    /**
     * Lấy customer_id tương ứng user đang login
     * users.username -> customers.customer_id
     */
    private Integer getCurrentCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String username = auth.getName();

        try {
            return jdbc.queryForObject(
                    """
                    SELECT c.customer_id
                    FROM customers c
                    JOIN users u ON u.user_id = c.user_id
                    WHERE u.username = ?
                    """,
                    Integer.class,
                    username
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    // POST /api/requests/{id}/images  (upload ảnh cho request đã tạo)
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,Object>> uploadImages(
            @PathVariable Integer id,
            @RequestParam(name = "images", required = false) List<MultipartFile> images) {

        int saved = fullRequestService.saveImagesToRequest(id, images == null ? List.of() : images);
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", Map.of("requestId", id, "saved", saved));
        return ResponseEntity.ok(body);
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
                    data.put("totalCost", sr.getTotalCost());
                    
                    // Include provider information if providerId exists
                    if (sr.getProviderId() != null) {
                        providerRepository.findById(sr.getProviderId()).ifPresent(provider -> {
                            Map<String, Object> providerInfo = new LinkedHashMap<>();
                            providerInfo.put("providerId", provider.getProviderId());
                            providerInfo.put("companyName", provider.getCompanyName());
                            providerInfo.put("rating", provider.getRating());
                            providerInfo.put("verificationStatus", provider.getVerificationStatus());
                            data.put("provider", providerInfo);
                        });
                    }
                    
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
