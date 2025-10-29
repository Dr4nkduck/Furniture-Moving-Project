package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="provider_service_packages",
        uniqueConstraints=@UniqueConstraint(columnNames={"provider_id","package_id"}))
public class ProviderServicePackage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Column(name="provider_id", nullable=false) private Integer providerId;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="package_id", nullable=false)
    private ServicePackage servicePackage;

    private BigDecimal baseFee;
    private BigDecimal perKm;
    private BigDecimal perMinute;
    private BigDecimal surchargeStairs;
    private BigDecimal surchargeNoElevator;
    private BigDecimal surchargeNarrowAlley;
    private BigDecimal surchargeWeekend;
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

    public ServicePackage getServicePackage() {
        return servicePackage;
    }

    public void setServicePackage(ServicePackage servicePackage) {
        this.servicePackage = servicePackage;
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(BigDecimal baseFee) {
        this.baseFee = baseFee;
    }

    public BigDecimal getPerKm() {
        return perKm;
    }

    public void setPerKm(BigDecimal perKm) {
        this.perKm = perKm;
    }

    public BigDecimal getPerMinute() {
        return perMinute;
    }

    public void setPerMinute(BigDecimal perMinute) {
        this.perMinute = perMinute;
    }

    public BigDecimal getSurchargeStairs() {
        return surchargeStairs;
    }

    public void setSurchargeStairs(BigDecimal surchargeStairs) {
        this.surchargeStairs = surchargeStairs;
    }

    public BigDecimal getSurchargeNoElevator() {
        return surchargeNoElevator;
    }

    public void setSurchargeNoElevator(BigDecimal surchargeNoElevator) {
        this.surchargeNoElevator = surchargeNoElevator;
    }

    public BigDecimal getSurchargeNarrowAlley() {
        return surchargeNarrowAlley;
    }

    public void setSurchargeNarrowAlley(BigDecimal surchargeNarrowAlley) {
        this.surchargeNarrowAlley = surchargeNarrowAlley;
    }

    public BigDecimal getSurchargeWeekend() {
        return surchargeWeekend;
    }

    public void setSurchargeWeekend(BigDecimal surchargeWeekend) {
        this.surchargeWeekend = surchargeWeekend;
    }
}
