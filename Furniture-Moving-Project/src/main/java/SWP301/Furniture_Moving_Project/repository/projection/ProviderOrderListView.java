package SWP301.Furniture_Moving_Project.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ProviderOrderListView {
    Integer getRequestId();
    String getCustomerName();
    LocalDate getPreferredDate();
    String getPickupAddress();
    String getDeliveryAddress();
    String getStatus();
    BigDecimal getTotalCost();
}
