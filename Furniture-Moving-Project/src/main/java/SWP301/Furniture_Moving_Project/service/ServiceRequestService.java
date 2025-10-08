package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.*;
import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import SWP301.Furniture_Moving_Project.model.RequestAddress;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceRequestService {
    
    private final ServiceRequestRepository serviceRequestRepository;
    
    @Autowired
    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
    }
    
    @Transactional
    public ServiceRequestResponseDTO createServiceRequest(CreateServiceRequestDTO dto) {
        // Validate input
        validateCreateRequest(dto);
        
        // Create ServiceRequest entity
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setCustomerId(dto.getCustomerId());
        serviceRequest.setProviderId(dto.getProviderId());
        serviceRequest.setPreferredDate(dto.getPreferredDate());
        serviceRequest.setPreferredTimeStart(dto.getPreferredTimeStart());
        serviceRequest.setPreferredTimeEnd(dto.getPreferredTimeEnd());
        serviceRequest.setSpecialRequirements(dto.getSpecialRequirements());
        
        // Add addresses
        if (dto.getAddresses() != null) {
            for (AddressDTO addressDTO : dto.getAddresses()) {
                RequestAddress address = mapToRequestAddress(addressDTO);
                serviceRequest.addAddress(address);
            }
        }
        
        // Add furniture items
        if (dto.getFurnitureItems() != null) {
            for (FurnitureItemDTO itemDTO : dto.getFurnitureItems()) {
                FurnitureItem item = mapToFurnitureItem(itemDTO);
                serviceRequest.addFurnitureItem(item);
            }
        }
        
        // Save to database
        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);
        
        // Convert to response DTO
        return mapToResponseDTO(savedRequest);
    }
    
    public ServiceRequestResponseDTO getRequestById(Integer requestId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
        return mapToResponseDTO(request);
    }
    
    public List<ServiceRequestResponseDTO> getRequestsByCustomerId(Integer customerId) {
        List<ServiceRequest> requests = serviceRequestRepository.findByCustomerId(customerId);
        return requests.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    
    public List<ServiceRequestResponseDTO> getRequestsByStatus(String status) {
        List<ServiceRequest> requests = serviceRequestRepository.findByStatus(status);
        return requests.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    
    public List<ServiceRequestResponseDTO> getAllRequests() {
        List<ServiceRequest> requests = serviceRequestRepository.findAll();
        return requests.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    
    // Validation methods
    private void validateCreateRequest(CreateServiceRequestDTO dto) {
        if (dto.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        
        if (dto.getPreferredDate() == null) {
            throw new IllegalArgumentException("Preferred date is required");
        }
        
        if (dto.getAddresses() == null || dto.getAddresses().isEmpty()) {
            throw new IllegalArgumentException("At least one address is required");
        }
        
        // Check for at least one pickup and one delivery address
        long pickupCount = dto.getAddresses().stream()
                .filter(a -> "Pickup".equalsIgnoreCase(a.getAddressType()))
                .count();
        long deliveryCount = dto.getAddresses().stream()
                .filter(a -> "Delivery".equalsIgnoreCase(a.getAddressType()))
                .count();
        
        if (pickupCount == 0) {
            throw new IllegalArgumentException("At least one Pickup address is required");
        }
        
        if (deliveryCount == 0) {
            throw new IllegalArgumentException("At least one Delivery address is required");
        }
        
        if (dto.getFurnitureItems() == null || dto.getFurnitureItems().isEmpty()) {
            throw new IllegalArgumentException("At least one furniture item is required");
        }
        
        // Validate addresses
        for (AddressDTO address : dto.getAddresses()) {
            validateAddress(address);
        }
        
        // Validate furniture items
        for (FurnitureItemDTO item : dto.getFurnitureItems()) {
            validateFurnitureItem(item);
        }
    }
    
    private void validateAddress(AddressDTO address) {
        if (address.getAddressType() == null || address.getAddressType().trim().isEmpty()) {
            throw new IllegalArgumentException("Address type is required");
        }
        
        if (address.getStreetAddress() == null || address.getStreetAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Street address is required");
        }
        
        if (address.getCity() == null || address.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        
        if (address.getState() == null || address.getState().trim().isEmpty()) {
            throw new IllegalArgumentException("State is required");
        }
        
        if (address.getZipCode() == null || address.getZipCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Zip code is required");
        }
    }
    
    private void validateFurnitureItem(FurnitureItemDTO item) {
        if (item.getItemType() == null || item.getItemType().trim().isEmpty()) {
            throw new IllegalArgumentException("Item type is required");
        }
        
        if (item.getQuantity() == null || item.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
    }
    
    // Mapping methods
    private RequestAddress mapToRequestAddress(AddressDTO dto) {
        RequestAddress address = new RequestAddress();
        address.setAddressType(dto.getAddressType());
        address.setStreetAddress(dto.getStreetAddress());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());
        address.setContactName(dto.getContactName());
        address.setContactPhone(dto.getContactPhone());
        address.setSpecialInstructions(dto.getSpecialInstructions());
        return address;
    }
    
    private FurnitureItem mapToFurnitureItem(FurnitureItemDTO dto) {
        FurnitureItem item = new FurnitureItem();
        item.setItemType(dto.getItemType());
        item.setItemName(dto.getItemName());
        item.setQuantity(dto.getQuantity());
        item.setSize(dto.getSize());
        item.setEstimatedWeight(dto.getEstimatedWeight());
        item.setIsFragile(dto.getIsFragile() != null ? dto.getIsFragile() : false);
        item.setRequiresDisassembly(dto.getRequiresDisassembly() != null ? dto.getRequiresDisassembly() : false);
        item.setSpecialHandlingNotes(dto.getSpecialHandlingNotes());
        return item;
    }
    
    private ServiceRequestResponseDTO mapToResponseDTO(ServiceRequest request) {
        ServiceRequestResponseDTO dto = new ServiceRequestResponseDTO();
        dto.setRequestId(request.getRequestId());
        dto.setCustomerId(request.getCustomerId());
        dto.setProviderId(request.getProviderId());
        dto.setRequestDate(request.getRequestDate());
        dto.setPreferredDate(request.getPreferredDate());
        dto.setPreferredTimeStart(request.getPreferredTimeStart());
        dto.setPreferredTimeEnd(request.getPreferredTimeEnd());
        dto.setStatus(request.getStatus());
        dto.setSpecialRequirements(request.getSpecialRequirements());
        dto.setCreatedAt(request.getCreatedAt());
        
        // Map addresses
        List<AddressDTO> addressDTOs = new ArrayList<>();
        for (RequestAddress address : request.getAddresses()) {
            AddressDTO addressDTO = new AddressDTO();
            addressDTO.setAddressType(address.getAddressType());
            addressDTO.setStreetAddress(address.getStreetAddress());
            addressDTO.setCity(address.getCity());
            addressDTO.setState(address.getState());
            addressDTO.setZipCode(address.getZipCode());
            addressDTO.setContactName(address.getContactName());
            addressDTO.setContactPhone(address.getContactPhone());
            addressDTO.setSpecialInstructions(address.getSpecialInstructions());
            addressDTOs.add(addressDTO);
        }
        dto.setAddresses(addressDTOs);
        
        // Map furniture items
        List<FurnitureItemDTO> itemDTOs = new ArrayList<>();
        for (FurnitureItem item : request.getFurnitureItems()) {
            FurnitureItemDTO itemDTO = new FurnitureItemDTO();
            itemDTO.setItemType(item.getItemType());
            itemDTO.setItemName(item.getItemName());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setSize(item.getSize());
            itemDTO.setEstimatedWeight(item.getEstimatedWeight());
            itemDTO.setIsFragile(item.getIsFragile());
            itemDTO.setRequiresDisassembly(item.getRequiresDisassembly());
            itemDTO.setSpecialHandlingNotes(item.getSpecialHandlingNotes());
            itemDTOs.add(itemDTO);
        }
        dto.setFurnitureItems(itemDTOs);
        
        return dto;
    }
}