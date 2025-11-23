package SWP301.Furniture_Moving_Project.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ProviderOrderDetailProjection {
    Integer getRequestId();
    String getStatus();
    LocalDateTime getRequestDate();
    LocalDate getPreferredDate();
    BigDecimal getTotalCost();

    String getCustomerFirstName();
    String getCustomerLastName();
    String getCustomerPhone();
    String getCustomerEmail();

    String getPickupStreet();
    String getPickupCity();
    String getPickupState();
    String getPickupZip();

    String getDeliveryStreet();
    String getDeliveryCity();
    String getDeliveryState();
    String getDeliveryZip();
}
