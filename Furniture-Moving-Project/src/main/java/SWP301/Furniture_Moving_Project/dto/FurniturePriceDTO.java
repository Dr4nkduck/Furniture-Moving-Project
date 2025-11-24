package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

public class FurniturePriceDTO {

    // ==== Trường chuẩn cho API mới (PV-002) ====
    private Integer furnitureItemId;      // dùng cho frontend: furnitureItemId
    private String furnitureItemName;     // dùng cho frontend: furnitureItemName
    private BigDecimal price;            // dùng chung

    // ====== Getter/Setter chuẩn (item*) ======
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

    // ====== ALIAS cho code cũ (type*) ======
    // ProviderPricingServiceImpl đang gọi mấy hàm này

    public Integer getFurnitureTypeId() {
        return furnitureItemId;
    }

    public void setFurnitureTypeId(Integer furnitureTypeId) {
        this.furnitureItemId = furnitureTypeId;
    }

    public String getFurnitureTypeName() {
        return furnitureItemName;
    }

    public void setFurnitureTypeName(String furnitureTypeName) {
        this.furnitureItemName = furnitureTypeName;
    }
}
