package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "provider_service_packages",
        uniqueConstraints = @UniqueConstraint(name = "uq_psp", columnNames = {"provider_id","package_id"})
)
public class ProviderServicePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private ServicePackage servicePackage;

    @Column(name = "per_km")
    private BigDecimal perKm; // chỉ giữ field này theo PV-002

    // getters/setters
    public Integer getId() { return id; }
    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }

    public ServicePackage getServicePackage() { return servicePackage; }
    public void setServicePackage(ServicePackage servicePackage) { this.servicePackage = servicePackage; }

    public BigDecimal getPerKm() { return perKm; }
    public void setPerKm(BigDecimal perKm) { this.perKm = perKm; }
}
