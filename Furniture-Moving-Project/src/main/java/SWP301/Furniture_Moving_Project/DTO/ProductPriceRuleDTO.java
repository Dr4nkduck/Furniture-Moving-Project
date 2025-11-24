package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

public class ProductPriceRuleDTO {

    private String baseName;
    private String sizeLabel;
    private Long unitPrice;
    private BigDecimal maxLengthCm;
    private BigDecimal maxWidthCm;
    private BigDecimal maxHeightCm;
    private BigDecimal maxWeightKg;

    // getters / setters

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getSizeLabel() {
        return sizeLabel;
    }

    public void setSizeLabel(String sizeLabel) {
        this.sizeLabel = sizeLabel;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getMaxLengthCm() {
        return maxLengthCm;
    }

    public void setMaxLengthCm(BigDecimal maxLengthCm) {
        this.maxLengthCm = maxLengthCm;
    }

    public BigDecimal getMaxWidthCm() {
        return maxWidthCm;
    }

    public void setMaxWidthCm(BigDecimal maxWidthCm) {
        this.maxWidthCm = maxWidthCm;
    }

    public BigDecimal getMaxHeightCm() {
        return maxHeightCm;
    }

    public void setMaxHeightCm(BigDecimal maxHeightCm) {
        this.maxHeightCm = maxHeightCm;
    }

    public BigDecimal getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(BigDecimal maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }
}
