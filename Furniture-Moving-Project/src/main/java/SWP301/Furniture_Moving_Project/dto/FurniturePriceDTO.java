package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

public class FurniturePriceDTO {
    public Integer furnitureItemId;   // furniture_type_id
    public String  furnitureItemName; // nếu thêm mới theo tên
    public BigDecimal price;

    public Integer getFurnitureItemId() {
        return furnitureItemId;
    }

    public void setFurnitureItemId(Integer furnitureItemId) {
        this.furnitureItemId = furnitureItemId;
    }

    public String getFurnitureItemName() {
        return furnitureItemName;
    }

    public void setFurnitureItemName(String furnitureItemName) {
        this.furnitureItemName = furnitureItemName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
