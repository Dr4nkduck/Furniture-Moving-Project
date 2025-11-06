package SWP301.Furniture_Moving_Project.dto;

public class FurniturePriceDTO {
    public Integer furnitureItemId;   // furniture_type_id
    public String  furnitureItemName; // nếu thêm mới theo tên
    public Double price;

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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
