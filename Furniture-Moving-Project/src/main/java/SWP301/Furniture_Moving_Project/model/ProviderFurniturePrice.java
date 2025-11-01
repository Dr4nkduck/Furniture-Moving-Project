package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "provider_furniture_prices",
        uniqueConstraints = @UniqueConstraint(name = "uk_pfp_provider_pkg_item",
                columnNames = {"provider_id", "service_package_id", "furniture_item_id"}),
        indexes = {
                @Index(name = "idx_pfp_provider", columnList = "provider_id"),
                @Index(name = "idx_pfp_package", columnList = "service_package_id"),
                @Index(name = "idx_pfp_item", columnList = "furniture_item_id")
        })
public class ProviderFurniturePrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pfp_provider"))
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_package_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pfp_service_package"))
    private ServicePackage servicePackage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "furniture_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pfp_furniture_item"))
    private FurnitureItem furnitureItem;

    @Column(name = "price", precision = 18, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // getters/setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public ServicePackage getServicePackage() {
        return servicePackage;
    }

    public void setServicePackage(ServicePackage servicePackage) {
        this.servicePackage = servicePackage;
    }

    public FurnitureItem getFurnitureItem() {
        return furnitureItem;
    }

    public void setFurnitureItem(FurnitureItem furnitureItem) {
        this.furnitureItem = furnitureItem;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
