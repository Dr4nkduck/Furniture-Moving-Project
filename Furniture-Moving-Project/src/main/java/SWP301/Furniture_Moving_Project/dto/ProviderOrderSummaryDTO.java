package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProviderOrderSummaryDTO {
    private Integer requestId;
    private String status;                  // pending / assigned / in_progress / completed / cancelled
    private LocalDate preferredDate;
    private String customerName;            // "First Last"
    private String pickupAddress;           // "street, city"
    private String deliveryAddress;         // "street, city"
    private BigDecimal totalCost;           // may be null

    public ProviderOrderSummaryDTO() {
    }

    public ProviderOrderSummaryDTO(Integer requestId, String status, LocalDate preferredDate,
                                   String customerName, String pickupAddress, String deliveryAddress,
                                   BigDecimal totalCost) {
        this.requestId = requestId;
        this.status = status;
        this.preferredDate = preferredDate;
        this.customerName = customerName;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.totalCost = totalCost;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public java.math.BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(java.math.BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
}
