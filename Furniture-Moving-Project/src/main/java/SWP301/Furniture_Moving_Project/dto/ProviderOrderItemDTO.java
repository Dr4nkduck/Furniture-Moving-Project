package SWP301.Furniture_Moving_Project.dto;

public class ProviderOrderItemDTO {
    private String itemType;     // furniture_items.item_type
    private String size;         // optional
    private Integer quantity;
    private boolean fragile;

    public ProviderOrderItemDTO() {
    }

    public ProviderOrderItemDTO(String itemType, String size, Integer quantity, boolean fragile) {
        this.itemType = itemType;
        this.size = size;
        this.quantity = quantity;
        this.fragile = fragile;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public boolean isFragile() {
        return fragile;
    }

    public void setFragile(boolean fragile) {
        this.fragile = fragile;
    }
}
