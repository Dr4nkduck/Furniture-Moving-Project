// src/main/java/SWP301/Furniture_Moving_Project/model/ProviderPackageFurniturePrice.java
package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity @Table(
        name="provider_package_furniture_prices",
        uniqueConstraints = @UniqueConstraint(columnNames={"provider_id","package_id","furniture_type_id"})
)
public class ProviderPackageFurniturePrice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="provider_id", nullable=false)
    private Integer providerId;

    @Column(name="package_id", nullable=false)
    private Integer packageId;

    @Column(name="furniture_type_id", nullable=false)
    private Integer furnitureTypeId;

    @Column(name="price", nullable=false)
    private Double price;

    // read-only join
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="furniture_type_id", insertable = false, updatable = false)
    private FurnitureType furnitureType;

    // getters/setters


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(Integer packageId) {
        this.packageId = packageId;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getFurnitureTypeId() {
        return furnitureTypeId;
    }

    public void setFurnitureTypeId(Integer furnitureTypeId) {
        this.furnitureTypeId = furnitureTypeId;
    }

    public FurnitureType getFurnitureType() {
        return furnitureType;
    }

    public void setFurnitureType(FurnitureType furnitureType) {
        this.furnitureType = furnitureType;
    }
}
