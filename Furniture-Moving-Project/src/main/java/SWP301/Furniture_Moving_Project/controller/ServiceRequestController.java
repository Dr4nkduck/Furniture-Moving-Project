package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.CreateServiceRequestDTO;
import SWP301.Furniture_Moving_Project.dto.ServiceRequestResponseDTO;
import SWP301.Furniture_Moving_Project.service.ServiceRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class ServiceRequestController {
    
    private final ServiceRequestService serviceRequestService;
    
    @Autowired
    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }
    
    /**
     * Create a new service request
     * POST /api/requests
     */
    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody CreateServiceRequestDTO requestDTO) {
        try {
            ServiceRequestResponseDTO response = serviceRequestService.createServiceRequest(requestDTO);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Request created successfully");
            result.put("data", response);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred while creating the request");
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get a request by ID
     * GET /api/requests/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(@PathVariable Integer id) {
        try {
            ServiceRequestResponseDTO response = serviceRequestService.getRequestById(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred while retrieving the request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get all requests
     * GET /api/requests
     */
    @GetMapping
    public ResponseEntity<?> getAllRequests(
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) String status) {
        try {
            List<ServiceRequestResponseDTO> requests;
            
            if (customerId != null && status != null) {
                // Filter by both customer and status (need to add this method to service)
                requests = serviceRequestService.getRequestsByCustomerId(customerId)
                        .stream()
                        .filter(r -> r.getStatus().equalsIgnoreCase(status))
                        .toList();
            } else if (customerId != null) {
                requests = serviceRequestService.getRequestsByCustomerId(customerId);
            } else if (status != null) {
                requests = serviceRequestService.getRequestsByStatus(status);
            } else {
                requests = serviceRequestService.getAllRequests();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", requests.size());
            result.put("data", requests);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred while retrieving requests");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get requests by customer ID
     * GET /api/requests/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getRequestsByCustomerId(@PathVariable Integer customerId) {
        try {
            List<ServiceRequestResponseDTO> requests = serviceRequestService.getRequestsByCustomerId(customerId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", requests.size());
            result.put("data", requests);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred while retrieving customer requests");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get requests by status
     * GET /api/requests/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getRequestsByStatus(@PathVariable String status) {
        try {
            List<ServiceRequestResponseDTO> requests = serviceRequestService.getRequestsByStatus(status);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", requests.size());
            result.put("data", requests);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred while retrieving requests by status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}