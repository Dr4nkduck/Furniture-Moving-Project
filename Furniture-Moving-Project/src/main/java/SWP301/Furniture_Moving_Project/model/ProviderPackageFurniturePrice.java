package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "provider_package_furniture_prices",
        uniqueConstraints = @UniqueConstraint(name = "UQ_ppfp", columnNames = {"provider_id", "package_id", "furniture_type_id"})
)
public class ProviderPackageFurniturePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private ServicePackage servicePackage;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "furniture_type_id", nullable = false)
    private FurnitureType furnitureType;

    @Column(nullable = false)
    private BigDecimal price;

    // getters/setters
    public Integer getId() {
        return id;
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

    public FurnitureType getFurnitureType() {
        return furnitureType;
    }

    public void setFurnitureType(FurnitureType furnitureType) {
        this.furnitureType = furnitureType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
