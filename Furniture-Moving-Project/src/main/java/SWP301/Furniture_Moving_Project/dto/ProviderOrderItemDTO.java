package SWP301.Furniture_Moving_Project.dto;

public class ProviderOrderItemDTO {
    private Integer itemId;
    private String itemType;
    private String size;
    private int quantity;
    private boolean fragile;

    public ProviderOrderItemDTO() {
    }

    public ProviderOrderItemDTO(Integer itemId, String itemType, String size, int quantity, boolean fragile) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.size = size;
        this.quantity = quantity;
        this.fragile = fragile;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isFragile() {
        return fragile;
    }

    public void setFragile(boolean fragile) {
        this.fragile = fragile;
    }
}
