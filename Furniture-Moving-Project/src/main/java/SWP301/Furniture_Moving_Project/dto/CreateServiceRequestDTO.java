package SWP301.Furniture_Moving_Project.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*; // @NotNull, @Size, @FutureOrPresent ...
public class CreateServiceRequestDTO {

    @NotNull(message = "customerId is required")
    private Integer customerId;

    private Integer providerId;

    @NotNull(message = "pickupAddressId is required")
    private Integer pickupAddressId;

    @NotNull(message = "deliveryAddressId is required")
    private Integer deliveryAddressId;

    @NotNull(message = "preferredDate is required")
    @FutureOrPresent(message = "preferredDate cannot be in the past")
    private LocalDate preferredDate;

    private String status;        // optional, default "pending"
    private BigDecimal totalCost; // optional

    @NotNull(message = "furnitureItems is required")
    @Size(min = 1, message = "At least 1 furniture item")
    private List<@Valid FurnitureItemDTO> furnitureItems;

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public Integer getProviderId() { return providerId; }
    public void setProviderId(Integer providerId) { this.providerId = providerId; }
    public Integer getPickupAddressId() { return pickupAddressId; }
    public void setPickupAddressId(Integer pickupAddressId) { this.pickupAddressId = pickupAddressId; }
    public Integer getDeliveryAddressId() { return deliveryAddressId; }
    public void setDeliveryAddressId(Integer deliveryAddressId) { this.deliveryAddressId = deliveryAddressId; }
    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public List<FurnitureItemDTO> getFurnitureItems() { return furnitureItems; }
    public void setFurnitureItems(List<FurnitureItemDTO> furnitureItems) { this.furnitureItems = furnitureItems; }
}
