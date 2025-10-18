package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    /** Actor ID (user/provider). We keep Integer because your older code expected Integer. */
    @Column(name = "user_id")
    private Integer userId;

    /** Short action key: ORDER_PII_READ, ORDER_STATUS_UPDATE, ORDER_ACCEPT, ... */
    @Column(name = "type", nullable = false, length = 64)
    private String type;

    /** Free-form details for auditing and debugging. */
    @Column(name = "details", columnDefinition = "nvarchar(max)")
    private String details;

    /** Optional origin IP if you want to capture it (nullable). */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** Creation timestamp. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    /* ================= Getters / Setters ================= */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
