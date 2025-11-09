package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers",
        indexes = {
                @Index(name = "IX_customers_user", columnList = "user_id")
        })
public class Customer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    // 1-1 với users (theo thiết kế của bạn)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_customers_user"),
            unique = true // đảm bảo mỗi user chỉ có 1 bản ghi customer
    )
    private User user;

    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints = 0;

    @Column(name = "customer_since", nullable = false)
    private LocalDateTime customerSince;

    /* ===== Constructors ===== */
    public Customer() { }

    public Customer(User user) {
        this.user = user;
    }

    public Customer(User user, int loyaltyPoints, LocalDateTime customerSince) {
        this.user = user;
        this.loyaltyPoints = loyaltyPoints;
        this.customerSince = customerSince;
    }

    /* ===== Lifecycle ===== */
    @PrePersist
    protected void onCreate() {
        if (customerSince == null) {
            customerSince = LocalDateTime.now(); // DB default là SYSUTCDATETIME(); ở đây set trước để tránh null
        }
    }

    /* ===== Getters/Setters ===== */
    public Integer getCustomerId() {
        return customerId;
    }
    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }
    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public LocalDateTime getCustomerSince() {
        return customerSince;
    }
    public void setCustomerSince(LocalDateTime customerSince) {
        this.customerSince = customerSince;
    }

    /* ===== equals/hashCode theo id ===== */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer that)) return false;
        return customerId != null && customerId.equals(that.customerId);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    /* ===== toString (ẩn field LAZY) ===== */
    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", userId=" + (user != null ? user.getUserId() : null) +
                ", loyaltyPoints=" + loyaltyPoints +
                ", customerSince=" + customerSince +
                '}';
    }
}
