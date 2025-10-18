package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.CreateServiceRequestDTO;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.ServiceRequestService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final ServiceRequestRepository serviceRequestRepository;

    public ServiceRequestController(ServiceRequestService serviceRequestService,
                                    ServiceRequestRepository serviceRequestRepository) {
        this.serviceRequestService = serviceRequestService;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    /** Create a new service request (customer → pickup/delivery + items) */
    @PostMapping
    public ResponseEntity<Long> createRequest(@RequestBody CreateServiceRequestDTO request) {
        Long id = serviceRequestService.create(request);
        return ResponseEntity.ok(id);
    }

    /** Fetch one request (basic view) */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequest> getRequest(@PathVariable Long id) {
        ServiceRequest sr = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ServiceRequest not found: id=" + id));
        return ResponseEntity.ok(sr);
    }

    // You can add more endpoints (list by customer, cancel, etc.) as needed
}
