package SWP301.Furniture_Moving_Project.model;

import SWP301.Furniture_Moving_Project.model.converter.OrderStatusAttributeConverter;
import SWP301.Furniture_Moving_Project.model.OrderStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    /* ===== Identity ===== */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    /* ===== Relations ===== */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_address_id")
    private Address pickupAddressEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_address_id")
    private Address deliveryAddressEntity;

    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FurnitureItem> furnitureItems = new ArrayList<>();

    /* ===== Core fields ===== */
    // maps DB column request_date
    @Column(name = "request_date", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @Convert(converter = OrderStatusAttributeConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    // maps DB column total_cost
    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /* ===== Route/ETA (optional columns; add via ALTER TABLE if missing) ===== */
    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "route_polyline")
    private String routePolyline;

    @Column(name = "notes")
    private String notes;

    @Column(name = "eta_text")
    private String etaText;

    /* ===== Lifecycle ===== */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (scheduledAt == null) scheduledAt = LocalDateTime.now();
        if (status == null) status = OrderStatus.PENDING_OFFER;
    }

    /* ===== Convenience getters for UI ===== */
    @Transient
    public String getPickupAddress() {
        return pickupAddressEntity != null ? pickupAddressEntity.toString() : null;
    }

    @Transient
    public String getDeliveryAddress() {
        return deliveryAddressEntity != null ? deliveryAddressEntity.toString() : null;
    }

    @Transient
    public String getCustomerName() {
        return customer != null ? customer.getFullName() : null;
    }

    @Transient
    public String getCustomerPhone() {
        return customer != null ? customer.getPhone() : null;
    }

    @Transient
    public String getCustomerEmail() {
        return customer != null ? customer.getEmail() : null;
    }

    /* ===== Getters / Setters ===== */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }

    public Address getPickupAddressEntity() { return pickupAddressEntity; }
    public void setPickupAddressEntity(Address pickupAddressEntity) { this.pickupAddressEntity = pickupAddressEntity; }

    public Address getDeliveryAddressEntity() { return deliveryAddressEntity; }
    public void setDeliveryAddressEntity(Address deliveryAddressEntity) { this.deliveryAddressEntity = deliveryAddressEntity; }

    public List<FurnitureItem> getFurnitureItems() { return furnitureItems; }
    public void setFurnitureItems(List<FurnitureItem> furnitureItems) { this.furnitureItems = furnitureItems; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }

    public String getRoutePolyline() { return routePolyline; }
    public void setRoutePolyline(String routePolyline) { this.routePolyline = routePolyline; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getEtaText() { return etaText; }
    public void setEtaText(String etaText) { this.etaText = etaText; }
}
