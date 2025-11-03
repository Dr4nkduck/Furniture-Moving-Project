package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.util.List;

public class PricingSaveRequestDTO {
    public Integer providerId;
    public Integer packageId;
    public BigDecimal pricePerKm;
    public List<FurniturePriceDTO> furniturePrices;


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
