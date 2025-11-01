package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

public class FurniturePriceDTO {
    private Integer furnitureItemId;
    private String furnitureItemName; // dùng để hiển thị
    private BigDecimal price;          // null => xóa giá riêng

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
