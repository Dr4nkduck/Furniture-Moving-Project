package SWP301.Furniture_Moving_Project.dto;

import SWP301.Furniture_Moving_Project.model.ProductPrice;
import java.math.BigDecimal;

public class ProductPriceItemDTO {

    // Tên hiển thị cho FE: "Giường đơn (Nhỏ)"
    private String name;

    // đơn giá
    private BigDecimal price;

    public ProductPriceItemDTO() {
    }

    public ProductPriceItemDTO(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    public static ProductPriceItemDTO fromEntity(ProductPrice e) {
        String displayName = e.getBaseName() + " (" + e.getSizeLabel() + ")";
        return new ProductPriceItemDTO(displayName, e.getUnitPrice());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
