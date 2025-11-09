package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

public class ProviderStatsDTO {
    private Integer providerId;
    private String companyName;
    private long jobsTotal;
    private long jobsCompleted;
    private long cancellations;
    private BigDecimal revenueTotal;      // DECIMAL(18,2)
    private BigDecimal acceptancePercent; // 0..100 (2 decimals)
    private BigDecimal avgRating;         // 0..5

    public ProviderStatsDTO() {}

    public ProviderStatsDTO(Integer providerId, String companyName, long jobsTotal, long jobsCompleted,
                            long cancellations, BigDecimal revenueTotal,
                            BigDecimal acceptancePercent, BigDecimal avgRating) {
        this.providerId = providerId;
        this.companyName = companyName;
        this.jobsTotal = jobsTotal;
        this.jobsCompleted = jobsCompleted;
        this.cancellations = cancellations;
        this.revenueTotal = revenueTotal;
        this.acceptancePercent = acceptancePercent;
        this.avgRating = avgRating;
    }

    public Integer getProviderId() { return providerId; }
    public void setProviderId(Integer providerId) { this.providerId = providerId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public long getJobsTotal() { return jobsTotal; }
    public void setJobsTotal(long jobsTotal) { this.jobsTotal = jobsTotal; }
    public long getJobsCompleted() { return jobsCompleted; }
    public void setJobsCompleted(long jobsCompleted) { this.jobsCompleted = jobsCompleted; }
    public long getCancellations() { return cancellations; }
    public void setCancellations(long cancellations) { this.cancellations = cancellations; }
    public BigDecimal getRevenueTotal() { return revenueTotal; }
    public void setRevenueTotal(BigDecimal revenueTotal) { this.revenueTotal = revenueTotal; }
    public BigDecimal getAcceptancePercent() { return acceptancePercent; }
    public void setAcceptancePercent(BigDecimal acceptancePercent) { this.acceptancePercent = acceptancePercent; }
    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }
}
