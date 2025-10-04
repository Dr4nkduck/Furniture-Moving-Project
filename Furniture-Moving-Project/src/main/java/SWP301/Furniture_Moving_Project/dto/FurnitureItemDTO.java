package SWP301.Furniture_Moving_Project.dto;

public class FurnitureItemDTO {
    private Long itemId;
    private String itemName;
    private String dimensions;
    private Integer quantity;
    private Boolean fragile;
    private Boolean disassemblyNeeded;

    public FurnitureItemDTO() {
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getFragile() {
        return fragile;
    }

    public void setFragile(Boolean fragile) {
        this.fragile = fragile;
    }

    public Boolean getDisassemblyNeeded() {
        return disassemblyNeeded;
    }

    public void setDisassemblyNeeded(Boolean disassemblyNeeded) {
        this.disassemblyNeeded = disassemblyNeeded;
    }
}