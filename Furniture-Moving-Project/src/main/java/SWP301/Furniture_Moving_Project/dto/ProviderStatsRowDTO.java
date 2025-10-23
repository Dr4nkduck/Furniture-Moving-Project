package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

public record ProviderStatsRowDTO(
        Long providerId,
        String providerName,
        Long jobs,
        BigDecimal acceptanceRate,   // 0..100
        Long cancellations,
        BigDecimal avgRating,        // 1..5
        BigDecimal revenueEstimate   // sum total_cost


) {
    @Override
    public Long providerId() {
        return providerId;
    }

    @Override
    public String providerName() {
        return providerName;
    }

    @Override
    public Long jobs() {
        return jobs;
    }

    @Override
    public BigDecimal acceptanceRate() {
        return acceptanceRate;
    }

    @Override
    public Long cancellations() {
        return cancellations;
    }

    @Override
    public BigDecimal avgRating() {
        return avgRating;
    }

    @Override
    public BigDecimal revenueEstimate() {
        return revenueEstimate;
    }
}
