package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers", schema = "dbo")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    // Giữ kiểu int thẳng, không cần @ManyToOne sang User để đỡ ràng buộc
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;

    @Column(name = "customer_since", columnDefinition = "datetime2")
    private LocalDateTime customerSince;

    @PrePersist
    public void prePersist() {
        if (customerSince == null) customerSince = LocalDateTime.now();
        if (loyaltyPoints == null) loyaltyPoints = 0;
    }

    // Getters & setters
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(Integer loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
    public LocalDateTime getCustomerSince() { return customerSince; }
    public void setCustomerSince(LocalDateTime customerSince) { this.customerSince = customerSince; }
}
