package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.*;
import SWP301.Furniture_Moving_Project.repository.AnalyticsRepository;
import SWP301.Furniture_Moving_Project.service.AdminAnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.StringJoiner;

@Service
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final AnalyticsRepository repo;
    public AdminAnalyticsServiceImpl(AnalyticsRepository repo) { this.repo = repo; }

    @Override
    public ProviderStatsResponseDTO providerStats(String fromIso, String toIso) {
        List<ProviderStatsRowDTO> rows = repo.providerStats(fromIso, toIso);
        return new ProviderStatsResponseDTO(fromIso, toIso, rows);
    }

    @Override
    public String providerStatsCsv(String fromIso, String toIso) {
        var data = providerStats(fromIso, toIso).rows();
        StringJoiner j = new StringJoiner("\n");
        j.add("provider_id,provider_name,jobs,acceptance_pct,cancellations,avg_rating,revenue_estimate");
        for (var r : data) {
            j.add(String.join(",",
                    safe(r.providerId()),
                    csv(r.providerName()),
                    safe(r.jobs()),
                    safe(r.acceptanceRate()),
                    safe(r.cancellations()),
                    safe(r.avgRating()),
                    safe(r.revenueEstimate())
            ));
        }
        return j.toString();
    }

    @Override
    public CustomerTrendsResponseDTO customerTrends(String fromIso, String toIso, int topCorridors) {
        CustomerFunnelDTO funnel = repo.funnel(fromIso, toIso);
        BigDecimal aov = repo.averageOrderValue(fromIso, toIso);
        var corridors = repo.topCorridors(fromIso, toIso, topCorridors);
        var reasons = repo.cancelReasons(fromIso, toIso);
        return new CustomerTrendsResponseDTO(fromIso, toIso, funnel, aov, corridors, reasons);
    }

    @Override
    public String customerTrendsCsv(String fromIso, String toIso, int topCorridors) {
        var trends = customerTrends(fromIso, toIso, topCorridors);
        StringJoiner out = new StringJoiner("\n");
        out.add("section,key,value");
        out.add("funnel,searches," + trends.funnel().searches());
        out.add("funnel,quotes," + trends.funnel().quotes());
        out.add("funnel,bookings," + trends.funnel().bookings());
        out.add("metric,average_order_value," + trends.averageOrderValue());

        out.add("corridors,header,from_city->to_city:jobs");
        for (var c : trends.topCorridors()) {
            out.add("corridor," + csv(c.fromCity() + "->" + c.toCity()) + "," + c.jobs());
        }

        if (trends.cancelReasons() != null && !trends.cancelReasons().isEmpty()) {
            out.add("cancellations,header,reason:count");
            for (var r : trends.cancelReasons()) {
                out.add("cancellation," + csv(r.reason()) + "," + r.count());
            }
        }
        return out.toString();
    }

    // --- CSV helpers
    private String csv(String s) {
        if (s == null) return "";
        String escaped = s.replace("\"","\"\"");
        return "\"" + escaped + "\"";
    }
    private String safe(Object o) {
        if (o == null) return "";
        if (o instanceof BigDecimal b) return b.stripTrailingZeros().toPlainString();
        return o.toString();
    }
}
