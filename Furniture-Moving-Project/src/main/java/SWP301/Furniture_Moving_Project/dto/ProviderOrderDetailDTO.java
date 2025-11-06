package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ProviderOrderDetailDTO {
    private Integer requestId;
    private String status;
    private LocalDate preferredDate;
    private String customerName;
    private String customerPhone;
    private String pickupAddress;
    private String deliveryAddress;
    private BigDecimal totalCost;
    private List<ProviderOrderItemDTO> items;

    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public List<ProviderOrderItemDTO> getItems() { return items; }
    public void setItems(List<ProviderOrderItemDTO> items) { this.items = items; }
}
