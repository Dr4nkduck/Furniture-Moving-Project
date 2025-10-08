package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

public class FurnitureItemDTO {
    
    private String itemType;
    private String itemName;
    private Integer quantity;
    private String size;
    private BigDecimal estimatedWeight;
    private Boolean isFragile;
    private Boolean requiresDisassembly;
    private String specialHandlingNotes;
    
    // Constructors
    public FurnitureItemDTO() {
    }
    
    // Getters and Setters
    public String getItemType() {
        return itemType;
    }
    
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }
    
    public BigDecimal getEstimatedWeight() {
        return estimatedWeight;
    }
    
    public void setEstimatedWeight(BigDecimal estimatedWeight) {
        this.estimatedWeight = estimatedWeight;
    }
    
    public Boolean getIsFragile() {
        return isFragile;
    }
    
    public void setIsFragile(Boolean isFragile) {
        this.isFragile = isFragile;
    }
    
    public Boolean getRequiresDisassembly() {
        return requiresDisassembly;
    }
    
    public void setRequiresDisassembly(Boolean requiresDisassembly) {
        this.requiresDisassembly = requiresDisassembly;
    }
    
    public String getSpecialHandlingNotes() {
        return specialHandlingNotes;
    }
    
    public void setSpecialHandlingNotes(String specialHandlingNotes) {
        this.specialHandlingNotes = specialHandlingNotes;
    }
}