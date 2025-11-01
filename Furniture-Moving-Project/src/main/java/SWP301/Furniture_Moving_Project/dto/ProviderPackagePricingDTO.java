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

}
