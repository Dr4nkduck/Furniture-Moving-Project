package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDetailDTO {
    private Long id;
    private String status;
    private LocalDateTime scheduledAt;
    private String pickup;
    private String dropoff;
    private BigDecimal distanceKm;
    private Integer durationMin;
    private BigDecimal totalPrice;
    private String notes;
    private String etaText;
    private String customerNameMasked;
    private String customerPhoneMasked;
    private String customerEmailMasked;
    private String routePolyline;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getPickup() { return pickup; }
    public void setPickup(String pickup) { this.pickup = pickup; }
    public String getDropoff() { return dropoff; }
    public void setDropoff(String dropoff) { this.dropoff = dropoff; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getEtaText() { return etaText; }
    public void setEtaText(String etaText) { this.etaText = etaText; }
    public String getCustomerNameMasked() { return customerNameMasked; }
    public void setCustomerNameMasked(String customerNameMasked) { this.customerNameMasked = customerNameMasked; }
    public String getCustomerPhoneMasked() { return customerPhoneMasked; }
    public void setCustomerPhoneMasked(String customerPhoneMasked) { this.customerPhoneMasked = customerPhoneMasked; }
    public String getCustomerEmailMasked() { return customerEmailMasked; }
    public void setCustomerEmailMasked(String customerEmailMasked) { this.customerEmailMasked = customerEmailMasked; }
    public String getRoutePolyline() { return routePolyline; }
    public void setRoutePolyline(String routePolyline) { this.routePolyline = routePolyline; }
}
