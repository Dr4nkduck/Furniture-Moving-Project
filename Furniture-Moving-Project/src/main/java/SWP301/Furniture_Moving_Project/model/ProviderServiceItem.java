package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "provider_service_items")
public class ProviderServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dịch vụ thuộc về Provider nào
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @Column(nullable = false, length = 120)
    private String name; // ví dụ: Xe bán tải nhỏ, Xe 3.5 tấn

    @Column(nullable = false)
    private BigDecimal baseFee; // phí mở đơn

    @Column(nullable = false)
    private BigDecimal perKm; // phí theo km

    @Column(nullable = false)
    private BigDecimal perMin; // phí theo phút

    // Phụ phí
    @Column(nullable = false)
    private BigDecimal surchargeStairs;       // không thang máy
    @Column(nullable = false)
    private BigDecimal surchargeNoElevator;   // vác bộ
    @Column(nullable = false)
    private BigDecimal surchargeNarrowAlley;  // ngõ hẹp
    @Column(nullable = false)
    private BigDecimal surchargeWeekend;      // cuối tuần

    @Column(nullable = false)
    private boolean active = true;

    // getters/setters
    public Long getId() {
        return id;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public BigDecimal getPerMin() {
        return perMin;
    }

    public void setPerMin(BigDecimal perMin) {
        this.perMin = perMin;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
