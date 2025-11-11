package SWP301.Furniture_Moving_Project.dto;

public class RateLimitConfigDto {
    private Integer userId;
    private Integer maxRequests;
    private Integer timeWindowSeconds;
    private String status;
    private String notes;

    public RateLimitConfigDto() {}

    public RateLimitConfigDto(Integer userId, Integer maxRequests, Integer timeWindowSeconds, String status, String notes) {
        this.userId = userId;
        this.maxRequests = maxRequests;
        this.timeWindowSeconds = timeWindowSeconds;
        this.status = status;
        this.notes = notes;
    }

    // Getters and Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getMaxRequests() { return maxRequests; }
    public void setMaxRequests(Integer maxRequests) { this.maxRequests = maxRequests; }

    public Integer getTimeWindowSeconds() { return timeWindowSeconds; }
    public void setTimeWindowSeconds(Integer timeWindowSeconds) { this.timeWindowSeconds = timeWindowSeconds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
