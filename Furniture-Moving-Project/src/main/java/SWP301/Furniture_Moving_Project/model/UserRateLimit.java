package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_rate_limits")
public class UserRateLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rate_limit_id")
    private Integer rateLimitId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // Maximum number of requests allowed
    @Column(name = "max_requests", nullable = false)
    private Integer maxRequests = 100;

    // Time window in seconds (e.g., 60 = 1 minute, 3600 = 1 hour)
    @Column(name = "time_window_seconds", nullable = false)
    private Integer timeWindowSeconds = 60;

    // enabled or disabled
    @Column(nullable = false)
    private String status = "enabled";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private Integer createdBy; // Admin user ID who created this limit

    @Column(columnDefinition = "NVARCHAR(500)")
    private String notes; // Admin notes

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Integer getRateLimitId() { return rateLimitId; }
    public void setRateLimitId(Integer rateLimitId) { this.rateLimitId = rateLimitId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getMaxRequests() { return maxRequests; }
    public void setMaxRequests(Integer maxRequests) { this.maxRequests = maxRequests; }

    public Integer getTimeWindowSeconds() { return timeWindowSeconds; }
    public void setTimeWindowSeconds(Integer timeWindowSeconds) { this.timeWindowSeconds = timeWindowSeconds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
