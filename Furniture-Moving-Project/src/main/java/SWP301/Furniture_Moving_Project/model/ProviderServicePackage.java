// src/main/java/SWP301/Furniture_Moving_Project/model/ProviderServicePackage.java
package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity @Table(
        name="provider_service_packages",
        uniqueConstraints = @UniqueConstraint(columnNames={"provider_id","package_id"})
)
public class ProviderServicePackage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="provider_id", nullable=false)
    private Integer providerId;

    @Column(name="package_id", nullable=false)
    private Integer packageId;

    @Column(name="per_km")
    private Double perKm; // có thể NULL (chưa cấu hình)

    @Column(name="package_name_snapshot")
    private String packageNameSnapshot; // NULL => dùng tên gốc

    // read-only join để lấy tên gói
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="package_id", insertable = false, updatable = false)
    private ServicePackage basePackage;

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

    public Double getPerKm() {
        return perKm;
    }

    public void setPerKm(Double perKm) {
        this.perKm = perKm;
    }

    public String getPackageNameSnapshot() {
        return packageNameSnapshot;
    }

    public void setPackageNameSnapshot(String packageNameSnapshot) {
        this.packageNameSnapshot = packageNameSnapshot;
    }

    public ServicePackage getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(ServicePackage basePackage) {
        this.basePackage = basePackage;
    }
}
