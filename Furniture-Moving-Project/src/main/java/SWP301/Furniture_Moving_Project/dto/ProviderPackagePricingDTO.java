package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProviderPackagePricingDTO {
    private Integer providerId;
    private Integer packageId;
    private String packageName;
    private BigDecimal pricePerKm;
    private List<FurniturePriceDTO> furniturePrices;

    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

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

    public BigDecimal getPricePerKm() {
        return pricePerKm;
    }

    public void setPricePerKm(BigDecimal pricePerKm) {
        this.pricePerKm = pricePerKm;
    }

    public List<FurniturePriceDTO> getFurniturePrices() {
        return furniturePrices;
    }

    public void setFurniturePrices(List<FurniturePriceDTO> furniturePrices) {
        this.furniturePrices = furniturePrices;
    }
}
