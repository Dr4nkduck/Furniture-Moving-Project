package SWP301.Furniture_Moving_Project.dto;

import jakarta.validation.constraints.*;

public class FurnitureItemDTO {

    @NotBlank(message = "itemType is required")
    private String itemType;

    // "Small"/"Medium"/"Large"/"Extra Large" (tùy bạn quy ước)
    private String size;

    // CHANGED: chuỗi số đo cụ thể, ví dụ "40x40x40 cm"
    // Không bắt buộc, nhưng khuyến nghị FE gửi.
    private String sizeDimensions;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be >= 1")
    private Integer quantity;

    @NotNull(message = "isFragile is required")
    private Boolean isFragile;

    private String specialHandling;

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getSizeDimensions() { return sizeDimensions; }
    public void setSizeDimensions(String sizeDimensions) { this.sizeDimensions = sizeDimensions; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Boolean getIsFragile() { return isFragile; }
    public void setIsFragile(Boolean isFragile) { this.isFragile = isFragile; }

    public String getSpecialHandling() { return specialHandling; }
    public void setSpecialHandling(String specialHandling) { this.specialHandling = specialHandling; }
}
