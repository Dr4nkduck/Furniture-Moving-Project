package SWP301.Furniture_Moving_Project.repository.projection;

import java.math.BigDecimal;

public interface ProviderStatsRowView {
    Long getProviderId();
    String getProviderName();
    Long getJobs();
    Long getCancellations();
    BigDecimal getRevenueEstimate();
    BigDecimal getAcceptancePct();   // 0..100
    BigDecimal getAvgRating();       // 1..5
}
