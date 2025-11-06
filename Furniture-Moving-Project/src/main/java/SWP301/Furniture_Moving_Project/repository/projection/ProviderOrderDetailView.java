package SWP301.Furniture_Moving_Project.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface ProviderOrderDetailView {
    Integer getRequestId();
    String  getCustomerName();
    String  getCustomerPhone();
    LocalDate getPreferredDate();
    OffsetDateTime getRequestDate();
    String  getPickupAddress();
    String  getDeliveryAddress();
    String  getStatus();
    BigDecimal getTotalCost();
}
