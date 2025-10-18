package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDetailDTO {
    public Long id;
    public String status;
    public LocalDateTime scheduledAt;
    public String pickup;
    public String dropoff;
    public BigDecimal distanceKm;
    public Integer durationMin;
    public BigDecimal totalPrice;
    public String notes;
    public String customerNameMasked;   // order-scoped info (mask PII)
    public String customerPhoneMasked;  // e.g., 09******23
    public String customerEmailMasked;  // e.g., a***@mail.com
    public String routePolyline;        // optional to draw route
    public String etaText;
}
