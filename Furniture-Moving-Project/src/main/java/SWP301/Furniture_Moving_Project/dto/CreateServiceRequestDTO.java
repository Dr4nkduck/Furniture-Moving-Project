package SWP301.Furniture_Moving_Project.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class CreateServiceRequestDTO {
    
    private Integer customerId;
    private Integer providerId;
    private LocalDate preferredDate;
    private LocalTime preferredTimeStart;
    private LocalTime preferredTimeEnd;
    private String specialRequirements;
    private List<AddressDTO> addresses;
    private List<FurnitureItemDTO> furnitureItems;
    
    // Constructors
    public CreateServiceRequestDTO() {
    }
    
    // Getters and Setters
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
}