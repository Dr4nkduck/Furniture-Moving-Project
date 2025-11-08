package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProviderOrderSummaryDTO {
    private Integer requestId;
    private String status;
    private LocalDateTime requestDate;
    private LocalDate preferredDate;
    private String customerName;
    private String pickupAddress;
    private String deliveryAddress;
    private BigDecimal totalCost;

    public ProviderOrderSummaryDTO() {
    }

    public ProviderOrderSummaryDTO(Integer requestId, String status, LocalDateTime requestDate,
                                   LocalDate preferredDate, String customerName,
                                   String pickupAddress, String deliveryAddress,
                                   BigDecimal totalCost) {
        this.requestId = requestId;
        this.status = status;
        this.requestDate = requestDate;
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

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
}
