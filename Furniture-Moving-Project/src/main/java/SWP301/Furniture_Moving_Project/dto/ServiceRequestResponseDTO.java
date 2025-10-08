package SWP301.Furniture_Moving_Project.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ServiceRequestResponseDTO {
    
    private Integer requestId;
    private Integer customerId;
    private Integer providerId;
    private LocalDateTime requestDate;
    private LocalDate preferredDate;
    private LocalTime preferredTimeStart;
    private LocalTime preferredTimeEnd;
    private String status;
    private String specialRequirements;
    private List<AddressDTO> addresses;
    private List<FurnitureItemDTO> furnitureItems;
    private LocalDateTime createdAt;
    
    // Constructors
    public ServiceRequestResponseDTO() {
    }
    
    // Getters and Setters
    public Integer getRequestId() {
        return requestId;
    }
    
    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }
    
    public Integer getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }
    
    public Integer getProviderId() {
        return providerId;
    }
    
    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }
    
    public LocalDateTime getRequestDate() {
        return requestDate;
    }
    
    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }
    
    public LocalDate getPreferredDate() {
        return preferredDate;
    }
    
    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }
    
    public LocalTime getPreferredTimeStart() {
        return preferredTimeStart;
    }
    
    public void setPreferredTimeStart(LocalTime preferredTimeStart) {
        this.preferredTimeStart = preferredTimeStart;
    }
    
    public LocalTime getPreferredTimeEnd() {
        return preferredTimeEnd;
    }
    
    public void setPreferredTimeEnd(LocalTime preferredTimeEnd) {
        this.preferredTimeEnd = preferredTimeEnd;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getSpecialRequirements() {
        return specialRequirements;
    }
    
    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }
    
    public List<AddressDTO> getAddresses() {
        return addresses;
    }
    
    public void setAddresses(List<AddressDTO> addresses) {
        this.addresses = addresses;
    }
    
    public List<FurnitureItemDTO> getFurnitureItems() {
        return furnitureItems;
    }
    
    public void setFurnitureItems(List<FurnitureItemDTO> furnitureItems) {
        this.furnitureItems = furnitureItems;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}