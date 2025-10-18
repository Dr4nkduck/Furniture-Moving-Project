package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

public class ProviderServiceItemDTO {
    private Long id;
    private String name;
    private BigDecimal baseFee;
    private BigDecimal perKm;
    private BigDecimal perMin;
    private BigDecimal surchargeStairs;
    private BigDecimal surchargeNoElevator;
    private BigDecimal surchargeNarrowAlley;
    private BigDecimal surchargeWeekend;
    private boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getBaseFee() { return baseFee; }
    public void setBaseFee(BigDecimal baseFee) { this.baseFee = baseFee; }
    public BigDecimal getPerKm() { return perKm; }
    public void setPerKm(BigDecimal perKm) { this.perKm = perKm; }
    public BigDecimal getPerMin() { return perMin; }
    public void setPerMin(BigDecimal perMin) { this.perMin = perMin; }
    public BigDecimal getSurchargeStairs() { return surchargeStairs; }
    public void setSurchargeStairs(BigDecimal surchargeStairs) { this.surchargeStairs = surchargeStairs; }
    public BigDecimal getSurchargeNoElevator() { return surchargeNoElevator; }
    public void setSurchargeNoElevator(BigDecimal surchargeNoElevator) { this.surchargeNoElevator = surchargeNoElevator; }
    public BigDecimal getSurchargeNarrowAlley() { return surchargeNarrowAlley; }
    public void setSurchargeNarrowAlley(BigDecimal surchargeNarrowAlley) { this.surchargeNarrowAlley = surchargeNarrowAlley; }
    public BigDecimal getSurchargeWeekend() { return surchargeWeekend; }
    public void setSurchargeWeekend(BigDecimal surchargeWeekend) { this.surchargeWeekend = surchargeWeekend; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
