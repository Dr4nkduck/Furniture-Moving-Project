package SWP301.Furniture_Moving_Project.dto;

import java.util.List;

public record ProviderStatsResponseDTO(
        String fromDate, String toDate,
        List<ProviderStatsRowDTO> rows
) {}
