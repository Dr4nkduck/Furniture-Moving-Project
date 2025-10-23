package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class AnalyticsRepository {

    private final JdbcTemplate jdbc;

    public AnalyticsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // -------- UC-A03: Provider Stats --------
    public List<ProviderStatsRowDTO> providerStats(String fromIso, String toIso) {

        // Jobs & revenue & cancellations per provider
        String jobsSql = """
            SELECT p.provider_id,
                   p.company_name,
                   COUNT(*) AS jobs,
                   SUM(CASE WHEN sr.status IN ('assigned','in_progress','completed') THEN ISNULL(sr.total_cost,0) ELSE 0 END) AS revenue_estimate,
                   SUM(CASE WHEN sr.status = 'cancelled' THEN 1 ELSE 0 END) AS cancellations
            FROM providers p
            LEFT JOIN service_requests sr ON sr.provider_id = p.provider_id
                 AND sr.created_at >= ? AND sr.created_at < ?
            GROUP BY p.provider_id, p.company_name
            """;

        // Quotes (total vs accepted) per provider via price_services
        String quoteSql = """
            SELECT ps.provider_id,
                   COUNT(*) AS total_quotes,
                   SUM(CASE WHEN pq.status = 'accepted' THEN 1 ELSE 0 END) AS accepted_quotes
            FROM price_quotations pq
            JOIN price_services ps ON ps.service_id = pq.service_id
            WHERE pq.created_at >= ? AND pq.created_at < ?
            GROUP BY ps.provider_id
            """;

        Map<Long, ProviderStatsRowDTO> rows = new LinkedHashMap<>();
        jdbc.query(jobsSql, rs -> {
            long pid = rs.getLong("provider_id");
            String name = rs.getString("company_name");
            long jobs = rs.getLong("jobs");
            long cancels = rs.getLong("cancellations");
            BigDecimal revenue = rs.getBigDecimal("revenue_estimate");
            rows.put(pid, new ProviderStatsRowDTO(pid, name, jobs, BigDecimal.ZERO, cancels,
                    null, revenue == null ? BigDecimal.ZERO : revenue));
        }, fromIso, toIso);

        // attach acceptance rate
        jdbc.query(quoteSql, rs -> {
            long pid = rs.getLong("provider_id");
            long total = rs.getLong("total_quotes");
            long acc = rs.getLong("accepted_quotes");
            BigDecimal rate = total == 0 ? BigDecimal.ZERO :
                    BigDecimal.valueOf(acc * 100.0 / total).setScale(2, BigDecimal.ROUND_HALF_UP);
            ProviderStatsRowDTO base = rows.get(pid);
            if (base != null) {
                rows.put(pid, new ProviderStatsRowDTO(
                        base.providerId(), base.providerName(),
                        base.jobs(), rate, base.cancellations(), base.avgRating(), base.revenueEstimate()
                ));
            } else {
                rows.put(pid, new ProviderStatsRowDTO(pid, null, 0L, rate, 0L, null, BigDecimal.ZERO));
            }
        }, fromIso, toIso);

        // attach avg rating (from reviews)
        String ratingSql = """
            SELECT provider_id, AVG(CAST(rating AS DECIMAL(10,2))) AS avg_rating
            FROM reviews
            WHERE created_at >= ? AND created_at < ?
            GROUP BY provider_id
            """;
        jdbc.query(ratingSql, rs -> {
            long pid = rs.getLong("provider_id");
            BigDecimal avg = rs.getBigDecimal("avg_rating");
            ProviderStatsRowDTO base = rows.get(pid);
            if (base != null) {
                rows.put(pid, new ProviderStatsRowDTO(
                        base.providerId(), base.providerName(),
                        base.jobs(), base.acceptanceRate(), base.cancellations(),
                        avg, base.revenueEstimate()
                ));
            } else {
                rows.put(pid, new ProviderStatsRowDTO(pid, null, 0L, BigDecimal.ZERO, 0L, avg, BigDecimal.ZERO));
            }
        }, fromIso, toIso);

        return new ArrayList<>(rows.values());
    }

    // -------- UC-A04: Customer Trends --------
    public CustomerFunnelDTO funnel(String fromIso, String toIso) {
        // Searches from activity_logs (requires event_type='SEARCH' to be logged)
        long searches = Optional.ofNullable(
                jdbc.queryForObject("""
                SELECT COUNT(*) FROM activity_logs
                WHERE event_type = 'SEARCH' AND timestamp >= ? AND timestamp < ?
            """, Long.class, fromIso, toIso)
        ).orElse(0L);

        long quotes = Optional.ofNullable(
                jdbc.queryForObject("""
                SELECT COUNT(*) FROM price_quotations
                WHERE created_at >= ? AND created_at < ?
            """, Long.class, fromIso, toIso)
        ).orElse(0L);

        long bookings = Optional.ofNullable(
                jdbc.queryForObject("""
                SELECT COUNT(*) FROM service_requests
                WHERE status IN ('assigned','in_progress','completed')
                  AND created_at >= ? AND created_at < ?
            """, Long.class, fromIso, toIso)
        ).orElse(0L);

        return new CustomerFunnelDTO(fromIso, toIso, searches, quotes, bookings);
    }

    public List<TopCorridorDTO> topCorridors(String fromIso, String toIso, int limit) {
        String sql = """
            SELECT TOP (?) a1.city AS from_city, a2.city AS to_city, COUNT(*) AS jobs
            FROM service_requests sr
            JOIN addresses a1 ON a1.address_id = sr.pickup_address_id
            JOIN addresses a2 ON a2.address_id = sr.delivery_address_id
            WHERE sr.created_at >= ? AND sr.created_at < ?
              AND sr.status IN ('assigned','in_progress','completed','cancelled')
            GROUP BY a1.city, a2.city
            ORDER BY jobs DESC
        """;
        return jdbc.query(sql, (rs, i) -> mapCorridor(rs), limit, fromIso, toIso);
    }

    public BigDecimal averageOrderValue(String fromIso, String toIso) {
        BigDecimal aov = jdbc.queryForObject("""
            SELECT AVG(CAST(total_cost AS DECIMAL(10,2))) AS aov
            FROM service_requests
            WHERE status IN ('assigned','in_progress','completed')
              AND created_at >= ? AND created_at < ?
        """, BigDecimal.class, fromIso, toIso);
        return aov == null ? BigDecimal.ZERO : aov;
    }

    public List<CustomerTrendsResponseDTO.CancelReasonBucket> cancelReasons(String fromIso, String toIso) {
        // Works only if cancel_reason column exists; otherwise returns empty list.
        try {
            String sql = """
                SELECT ISNULL(cancel_reason,'(unknown)') AS reason, COUNT(*) AS cnt
                FROM service_requests
                WHERE status = 'cancelled'
                  AND created_at >= ? AND created_at < ?
                GROUP BY cancel_reason
                ORDER BY cnt DESC
            """;
            return jdbc.query(sql, (rs, i) ->
                            new CustomerTrendsResponseDTO.CancelReasonBucket(rs.getString("reason"), rs.getLong("cnt")),
                    fromIso, toIso);
        } catch (Exception e) {
            return List.of(); // column might not exist yet
        }
    }

    private TopCorridorDTO mapCorridor(ResultSet rs) throws SQLException {
        return new TopCorridorDTO(rs.getString("from_city"), rs.getString("to_city"), rs.getLong("jobs"));
    }
}
