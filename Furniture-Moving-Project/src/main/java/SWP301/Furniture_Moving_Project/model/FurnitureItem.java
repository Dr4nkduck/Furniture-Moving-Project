package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "furniture_items")
public class FurnitureItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest serviceRequest;
    
    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;
    
    @Column(name = "item_name", length = 100)
    private String itemName;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "size", length = 50)
    private String size;
    
    @Column(name = "estimated_weight", precision = 6, scale = 2)
    private BigDecimal estimatedWeight;
    
    @Column(name = "is_fragile")
    private Boolean isFragile;
    
    @Column(name = "requires_disassembly")
    private Boolean requiresDisassembly;
    
    @Column(name = "special_handling_notes", columnDefinition = "TEXT")
    private String specialHandlingNotes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public FurnitureItem() {
        this.quantity = 1;
        this.isFragile = false;
        this.requiresDisassembly = false;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getItemId() {
        return itemId;
    }
    
    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }
    
    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }
    
    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }
    
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}