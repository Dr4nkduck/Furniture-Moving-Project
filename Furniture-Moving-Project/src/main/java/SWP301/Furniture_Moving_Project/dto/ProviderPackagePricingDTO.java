package SWP301.Furniture_Moving_Project.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProviderPackagePricingDTO {
    @NotNull
    private Integer packageId;
    private String packageName;

    @NotNull
    private BigDecimal baseFee;
    @NotNull
    private BigDecimal perKm;
    @NotNull
    private BigDecimal perMinute;
    @NotNull
    private BigDecimal surchargeStairs;
    @NotNull
    private BigDecimal surchargeNoElevator;
    @NotNull
    private BigDecimal surchargeNarrowAlley;
    @NotNull
    private BigDecimal surchargeWeekend;

    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(Integer packageId) {
        this.packageId = packageId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

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

    public BigDecimal getSurchargeStairs() {
        return surchargeStairs;
    }

    public void setSurchargeStairs(BigDecimal surchargeStairs) {
        this.surchargeStairs = surchargeStairs;
    }

    public BigDecimal getSurchargeNoElevator() {
        return surchargeNoElevator;
    }

    public void setSurchargeNoElevator(BigDecimal surchargeNoElevator) {
        this.surchargeNoElevator = surchargeNoElevator;
    }

    public BigDecimal getSurchargeNarrowAlley() {
        return surchargeNarrowAlley;
    }

    public void setSurchargeNarrowAlley(BigDecimal surchargeNarrowAlley) {
        this.surchargeNarrowAlley = surchargeNarrowAlley;
    }

    public BigDecimal getSurchargeWeekend() {
        return surchargeWeekend;
    }

    public void setSurchargeWeekend(BigDecimal surchargeWeekend) {
        this.surchargeWeekend = surchargeWeekend;
    }
}
