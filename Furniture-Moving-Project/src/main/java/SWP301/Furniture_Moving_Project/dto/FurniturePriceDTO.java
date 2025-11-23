package SWP301.Furniture_Moving_Project.dto;

public class FurniturePriceDTO {
    private Integer furnitureTypeId;
    private String furnitureTypeName;
    private Double price;

    public Integer getFurnitureTypeId() {
        return furnitureTypeId;
    }

    public void setFurnitureTypeId(Integer furnitureTypeId) {
        this.furnitureTypeId = furnitureTypeId;
    }

    public String getFurnitureTypeName() {
        return furnitureTypeName;
    }

    public void setFurnitureTypeName(String furnitureTypeName) {
        this.furnitureTypeName = furnitureTypeName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
