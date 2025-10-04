package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "furniture_items")
public class FurnitureItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @JsonIgnore
    private ServiceRequest request;
    
    @Column(name = "item_name", length = 100, nullable = false)
    private String itemName;
    
    @Column(name = "dimensions", length = 50)
    private String dimensions;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "fragile")
    private Boolean fragile = false;
    
    @Column(name = "disassembly_needed")
    private Boolean disassemblyNeeded = false;

    // Constructors
    public FurnitureItem() {
    }

    public FurnitureItem(ServiceRequest request, String itemName, Integer quantity) {
        this.request = request;
        this.itemName = itemName;
        this.quantity = quantity;
    }

    // Getters and Setters
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public ServiceRequest getRequest() {
        return request;
    }

    public void setRequest(ServiceRequest request) {
        this.request = request;
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