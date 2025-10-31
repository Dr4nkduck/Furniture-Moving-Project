package SWP301.Furniture_Moving_Project.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class FurniturePriceDTO {
    @NotNull
    private Integer furnitureTypeId;
    private String furnitureName; // for UI
    private String unit;          // for UI
    @NotNull
    private BigDecimal price;

    public Integer getFurnitureTypeId() {
        return furnitureTypeId;
    }

    public void setFurnitureTypeId(Integer furnitureTypeId) {
        this.furnitureTypeId = furnitureTypeId;
    }

    public String getFurnitureName() {
        return furnitureName;
    }

    public void setFurnitureName(String furnitureName) {
        this.furnitureName = furnitureName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
