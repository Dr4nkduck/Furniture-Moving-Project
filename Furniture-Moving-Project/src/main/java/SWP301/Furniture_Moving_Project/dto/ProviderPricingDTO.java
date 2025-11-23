package SWP301.Furniture_Moving_Project.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ProviderPricingDTO {
    @NotNull private BigDecimal baseFee;
    @NotNull private BigDecimal perKm;
    @NotNull private BigDecimal perMinute;


    // getters/setters


    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(BigDecimal baseFee) {
        this.baseFee = baseFee;
    }

    public BigDecimal getPerKm() {
        return perKm;
    }

    public void setPerKm(BigDecimal perKm) {
        this.perKm = perKm;
    }

    public BigDecimal getPerMinute() {
        return perMinute;
    }

    public void setPerMinute(BigDecimal perMinute) {
        this.perMinute = perMinute;
    }


}
