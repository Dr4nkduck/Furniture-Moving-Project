package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ProviderOrderDetailDTO {
    private Integer requestId;
    private String status;
    private LocalDateTime requestDate;
    private LocalDate preferredDate;

    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private String pickupFull;
    private String deliveryFull;

    private BigDecimal totalCostEstimate;

    private List<ProviderOrderItemDTO> items;

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

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getPickupFull() {
        return pickupFull;
    }

    public void setPickupFull(String pickupFull) {
        this.pickupFull = pickupFull;
    }

    public String getDeliveryFull() {
        return deliveryFull;
    }

    public void setDeliveryFull(String deliveryFull) {
        this.deliveryFull = deliveryFull;
    }

    public BigDecimal getTotalCostEstimate() {
        return totalCostEstimate;
    }

    public void setTotalCostEstimate(BigDecimal totalCostEstimate) {
        this.totalCostEstimate = totalCostEstimate;
    }

    public List<ProviderOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ProviderOrderItemDTO> items) {
        this.items = items;
    }
}
