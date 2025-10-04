package SWP301.Furniture_Moving_Project.dto;

import java.time.LocalDate;
import java.util.List;

public class CreateRequestDTO {
    private Long customerId;
    private Long providerId;
    private LocalDate preferredDate;
    private String preferredTime;
    private String specialRequirements;
    private AddressDTO pickupAddress;
    private AddressDTO deliveryAddress;
    private List<FurnitureItemDTO> furnitureItems;

    public CreateRequestDTO() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }

    public String getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(String preferredTime) {
        this.preferredTime = preferredTime;
    }

    public String getSpecialRequirements() {
        return specialRequirements;
    }

    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }

    public AddressDTO getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(AddressDTO pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public AddressDTO getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(AddressDTO deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public List<FurnitureItemDTO> getFurnitureItems() {
        return furnitureItems;
    }

    public void setFurnitureItems(List<FurnitureItemDTO> furnitureItems) {
        this.furnitureItems = furnitureItems;
    }
}