package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "provider_id", nullable = false)
    private Long providerId;
    
    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;
    
    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;
    
    @Column(name = "preferred_time", length = 20)
    private String preferredTime;
    
    @Column(name = "status", length = 20, nullable = false)
    private String status = "Pending";
    
    @Column(name = "special_requirements")
    private String specialRequirements;
    
    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;
    
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestAddress> addresses = new ArrayList<>();
    
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FurnitureItem> furnitureItems = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        requestDate = LocalDateTime.now();
        if (status == null) {
            status = "Pending";
        }
    }

    // Constructors
    public ServiceRequest() {
    }

    public ServiceRequest(Long customerId, Long providerId, LocalDate preferredDate, String preferredTime) {
        this.customerId = customerId;
        this.providerId = providerId;
        this.preferredDate = preferredDate;
        this.preferredTime = preferredTime;
    }

    // Getters and Setters
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
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

    public String getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(String preferredTime) {
        this.preferredTime = preferredTime;
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

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
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
}
