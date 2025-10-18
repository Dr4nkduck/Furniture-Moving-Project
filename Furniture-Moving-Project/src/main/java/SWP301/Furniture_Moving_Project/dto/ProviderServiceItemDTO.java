package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

public class ProviderServiceItemDTO {
    public Long id;
    public String name;
    public BigDecimal baseFee;
    public BigDecimal perKm;
    public BigDecimal perMin;
    public BigDecimal surchargeStairs;
    public BigDecimal surchargeNoElevator;
    public BigDecimal surchargeNarrowAlley;
    public BigDecimal surchargeWeekend;
    public boolean active;
}
