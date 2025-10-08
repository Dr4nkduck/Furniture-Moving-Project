package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;
    
    @Column(name = "customer_id", nullable = false)
    private Integer customerId;
    
    @Column(name = "provider_id")
    private Integer providerId; // References service_providers.provider_id
    
    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;
    
    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;
    
    @Column(name = "preferred_time_start")
    private LocalTime preferredTimeStart;
    
    @Column(name = "preferred_time_end")
    private LocalTime preferredTimeEnd;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    
    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;
    
    @Column(name = "estimated_distance", precision = 8, scale = 2)
    private BigDecimal estimatedDistance;
    
    @Column(name = "estimated_duration")
    private Integer estimatedDuration;
    
    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestAddress> addresses = new ArrayList<>();
    
    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FurnitureItem> furnitureItems = new ArrayList<>();
    
    // Constructors
    public ServiceRequest() {
        this.requestDate = LocalDateTime.now();
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getRequestId() {
        return requestId;
    }
    
    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }
    
    public Integer getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }
    
    public Integer getProviderId() {
        return providerId;
    }
    
    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }
    
    public LocalDateTime getRequestDate() {
        return requestDate;
    }
    
    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }
    
    public LocalDate getPreferredDate() {
        return preferredDate;
    }
    
    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }
    
    public LocalTime getPreferredTimeStart() {
        return preferredTimeStart;
    }
    
    public void setPreferredTimeStart(LocalTime preferredTimeStart) {
        this.preferredTimeStart = preferredTimeStart;
    }
    
    public LocalTime getPreferredTimeEnd() {
        return preferredTimeEnd;
    }
    
    public void setPreferredTimeEnd(LocalTime preferredTimeEnd) {
        this.preferredTimeEnd = preferredTimeEnd;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getSpecialRequirements() {
        return specialRequirements;
    }
    
    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }
    
    public BigDecimal getEstimatedDistance() {
        return estimatedDistance;
    }
    
    public void setEstimatedDistance(BigDecimal estimatedDistance) {
        this.estimatedDistance = estimatedDistance;
    }
    
    public Integer getEstimatedDuration() {
        return estimatedDuration;
    }
    
    public void setEstimatedDuration(Integer estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }
    
    public BigDecimal getTotalCost() {
        return totalCost;
    }
    
    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<RequestAddress> getAddresses() {
        return addresses;
    }
    
    public void setAddresses(List<RequestAddress> addresses) {
        this.addresses = addresses;
    }
    
    public List<FurnitureItem> getFurnitureItems() {
        return furnitureItems;
    }
    
    public void setFurnitureItems(List<FurnitureItem> furnitureItems) {
        this.furnitureItems = furnitureItems;
    }
    
    // Helper methods
    public void addAddress(RequestAddress address) {
        addresses.add(address);
        address.setServiceRequest(this);
    }
    
    public void addFurnitureItem(FurnitureItem item) {
        furnitureItems.add(item);
        item.setServiceRequest(this);
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}