package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "providers", schema = "dbo")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    private Integer providerId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "verification_status", nullable = false, length = 20)
    private String verificationStatus; // pending/verified/rejected

    @Column(name = "rating", precision = 3, scale = 2, nullable = false)
    private BigDecimal rating;

    @Column(name = "total_reviews", nullable = false)
    private Integer totalReviews;

    public Provider() {
    }

    @PrePersist
    public void prePersist() {
        if (verificationStatus == null || verificationStatus.isBlank()) {
            verificationStatus = "pending";
        }
        if (rating == null) {
            rating = new BigDecimal("0.00");
        }
        if (totalReviews == null) {
            totalReviews = 0;
        }
    }

    // ===== Getters & Setters =====
    public Integer getProviderId() {
        return providerId;
    }
    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }
    public Integer getUserId() {
        return userId;
    }
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    public String getVerificationStatus() {
        return verificationStatus;
    }
    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
    public BigDecimal getRating() {
        return rating;
    }
    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }
    public Integer getTotalReviews() {
        return totalReviews;
    }
    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }
}
