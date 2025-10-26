package SWP301.Furniture_Moving_Project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateServiceRequestDTO {

    @NotNull
    private Integer customerId;

    private Integer providerId;

    @NotNull
    private Integer pickupAddressId;

    @NotNull
    private Integer deliveryAddressId;

    @NotNull
    private LocalDate preferredDate;

    private String status;
    private BigDecimal totalCost;

    @NotEmpty @Valid
    private List<FurnitureItemDTO> furnitureItems;

    // getters/setters
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
