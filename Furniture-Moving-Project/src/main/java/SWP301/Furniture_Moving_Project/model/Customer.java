package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    // DB side là 1-1 với users; dùng OneToOne để rõ ràng (UNIQUE KEY ở DB đã bảo đảm)
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;

    @Column(name = "customer_since", nullable = false)
    private OffsetDateTime customerSince = OffsetDateTime.now();

    // getters/setters
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

    public Integer getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(Integer loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public OffsetDateTime getCustomerSince() {
        return customerSince;
    }

    public void setCustomerSince(OffsetDateTime customerSince) {
        this.customerSince = customerSince;
    }
}
