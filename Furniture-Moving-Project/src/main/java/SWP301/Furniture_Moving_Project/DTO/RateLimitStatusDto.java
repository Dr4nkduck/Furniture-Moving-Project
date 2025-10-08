package SWP301.Furniture_Moving_Project.DTO;

public class RateLimitStatusDto {
    private Integer userId;
    private String username;
    private Integer maxRequests;
    private Integer timeWindowSeconds;
    private Long currentRequestCount;
    private Long remainingRequests;
    private String status;
    private String notes;

    public RateLimitStatusDto() {}

    // Getters and Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getMaxRequests() { return maxRequests; }
    public void setMaxRequests(Integer maxRequests) { this.maxRequests = maxRequests; }

    public Integer getTimeWindowSeconds() { return timeWindowSeconds; }
    public void setTimeWindowSeconds(Integer timeWindowSeconds) { this.timeWindowSeconds = timeWindowSeconds; }

    public Long getCurrentRequestCount() { return currentRequestCount; }
    public void setCurrentRequestCount(Long currentRequestCount) { this.currentRequestCount = currentRequestCount; }

    public Long getRemainingRequests() { return remainingRequests; }
    public void setRemainingRequests(Long remainingRequests) { this.remainingRequests = remainingRequests; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
