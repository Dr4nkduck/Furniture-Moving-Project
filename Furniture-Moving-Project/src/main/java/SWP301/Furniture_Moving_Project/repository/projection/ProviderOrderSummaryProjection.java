package SWP301.Furniture_Moving_Project.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ProviderOrderSummaryProjection {
    Integer getRequestId();
    String getStatus();
    LocalDateTime getRequestDate();
    LocalDate getPreferredDate();

    String getCustomerFirstName();
    String getCustomerLastName();

    String getPickupStreet();
    String getPickupCity();
    String getDeliveryStreet();
    String getDeliveryCity();

    BigDecimal getTotalCost();
}
