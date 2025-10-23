package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.util.List;

public record CustomerTrendsResponseDTO(
        String fromDate, String toDate,
        CustomerFunnelDTO funnel,
        BigDecimal averageOrderValue,
        List<TopCorridorDTO> topCorridors,
        List<CancelReasonBucket> cancelReasons
) {
    public record CancelReasonBucket(String reason, long count) {}
}
