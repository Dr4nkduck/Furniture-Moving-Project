package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSummaryDTO {
    public Long id;
    public String status;          // PENDING_OFFER / ASSIGNED / IN_PROGRESS / COMPLETED / CANCELLED
    public String pickup;
    public String dropoff;
    public LocalDateTime scheduledAt;
    public BigDecimal totalPrice;
    public String routeSummary;    // short string, e.g. "12.4 km / 36 min"
}
