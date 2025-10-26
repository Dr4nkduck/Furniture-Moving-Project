package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_rate_limit_logs", indexes = {
    @Index(name = "idx_user_timestamp", columnList = "user_id,request_timestamp")
})
public class UserRateLimitLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "request_timestamp", nullable = false)
    private OffsetDateTime requestTimestamp;

    @Column(name = "request_path", columnDefinition = "NVARCHAR(500)")
    private String requestPath;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "was_blocked")
    private boolean wasBlocked = false;

    @PrePersist
    protected void onCreate() {
        requestTimestamp = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public OffsetDateTime getRequestTimestamp() { return requestTimestamp; }
    public void setRequestTimestamp(OffsetDateTime requestTimestamp) { this.requestTimestamp = requestTimestamp; }

    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public boolean isWasBlocked() { return wasBlocked; }
    public void setWasBlocked(boolean wasBlocked) { this.wasBlocked = wasBlocked; }
}
