package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.*;

import java.util.List;

public interface AdminAnalyticsService {
    ProviderStatsResponseDTO providerStats(String fromIso, String toIso);
    String providerStatsCsv(String fromIso, String toIso);

    CustomerTrendsResponseDTO customerTrends(String fromIso, String toIso, int topCorridors);
    String customerTrendsCsv(String fromIso, String toIso, int topCorridors);
}
