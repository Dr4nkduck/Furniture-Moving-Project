package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CheckoutRequest {
    private Integer customerId;
    private AddressPayload pickupAddress;
    private AddressPayload deliveryAddress;
    private String serviceType;
    private Boolean hasElevator;
    private LocalDate preferredDate;
    private BigDecimal totalCost;
    private List<FurnitureItemPayload> furnitureItems;

    public static class AddressPayload {
        public String streetAddress;
        public String city;
        public String state;
        public String zipCode;
    }

    public static class FurnitureItemPayload {
        public String itemType;
        public String size;
        public Integer quantity;
        public Boolean fragile;
        public String specialHandling;
    }

    // getters & setters
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public AddressPayload getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(AddressPayload pickupAddress) { this.pickupAddress = pickupAddress; }
    public AddressPayload getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(AddressPayload deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public Boolean getHasElevator() { return hasElevator; }
    public void setHasElevator(Boolean hasElevator) { this.hasElevator = hasElevator; }
    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public List<FurnitureItemPayload> getFurnitureItems() { return furnitureItems; }
    public void setFurnitureItems(List<FurnitureItemPayload> furnitureItems) { this.furnitureItems = furnitureItems; }
}
