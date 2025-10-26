package SWP301.Furniture_Moving_Project.repository.impl;

import SWP301.Furniture_Moving_Project.repository.AdminAnalyticsRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AdminAnalyticsRepositoryImpl implements AdminAnalyticsRepository {

    private final JdbcTemplate jdbc;
    public AdminAnalyticsRepositoryImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // ---------- UC-A03 ----------
    @Override
    @Transactional(readOnly = true)
    public List<Object[]> providerStats(LocalDateTime from, LocalDateTime to, String nameLike) {
        final String sql =
                "SELECT p.provider_id, p.company_name, " +
                        "       COALESCE(sr.jobs_total,0)       AS jobs_total, " +
                        "       COALESCE(sr.jobs_completed,0)   AS jobs_completed, " +
                        "       COALESCE(sr.cancellations,0)    AS cancellations, " +
                        "       COALESCE(sr.revenue_total,0)    AS revenue_total, " +
                        "       CASE WHEN pq.total_quotes > 0 THEN (100.0 * pq.accepted_quotes / pq.total_quotes) ELSE NULL END AS acceptance_percent, " +
                        "       COALESCE(rv.avg_rating,0)       AS avg_rating " +
                        "FROM providers p " +
                        "LEFT JOIN ( " +
                        "  SELECT provider_id, " +
                        "         SUM(CASE WHEN status IN ('assigned','in_progress','completed') THEN 1 ELSE 0 END) AS jobs_total, " +
                        "         SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) AS jobs_completed, " +
                        "         SUM(CASE WHEN status = 'cancelled' THEN 1 ELSE 0 END) AS cancellations, " +
                        "         SUM(CASE WHEN status = 'completed' AND total_cost IS NOT NULL THEN total_cost ELSE 0 END) AS revenue_total " +
                        "  FROM service_requests WHERE created_at >= ? AND created_at <= ? GROUP BY provider_id " +
                        ") sr ON sr.provider_id = p.provider_id " +
                        "LEFT JOIN ( " +
                        "  SELECT ps.provider_id, COUNT(*) AS total_quotes, " +
                        "         SUM(CASE WHEN pq.status = 'accepted' THEN 1 ELSE 0 END) AS accepted_quotes " +
                        "  FROM price_quotations pq JOIN price_services ps ON ps.service_id = pq.service_id " +
                        "  WHERE pq.created_at >= ? AND pq.created_at <= ? GROUP BY ps.provider_id " +
                        ") pq ON pq.provider_id = p.provider_id " +
                        "LEFT JOIN ( " +
                        "  SELECT r.provider_id, AVG(r.rating * 1.0) AS avg_rating " +
                        "  FROM reviews r WHERE r.created_at >= ? AND r.created_at <= ? GROUP BY r.provider_id " +
                        ") rv ON rv.provider_id = p.provider_id " +
                        "WHERE (? IS NULL OR p.company_name LIKE ?) " +
                        "ORDER BY revenue_total DESC, jobs_completed DESC";

        Timestamp f = Timestamp.valueOf(from), t = Timestamp.valueOf(to);
        String like = (nameLike == null || nameLike.isBlank()) ? null : "%" + nameLike.trim() + "%";

        return jdbc.query(sql, (rs, rowNum) -> new Object[]{
                rs.getInt("provider_id"),
                rs.getString("company_name"),
                rs.getLong("jobs_total"),
                rs.getLong("jobs_completed"),
                rs.getLong("cancellations"),
                rs.getBigDecimal("revenue_total"),
                rs.getBigDecimal("acceptance_percent"),
                rs.getBigDecimal("avg_rating")
        }, f, t, f, t, f, t, like, like);
    }

    // ---------- UC-A04 (daily; rolled up in service) ----------
    @Override @Transactional(readOnly = true)
    public List<Object[]> searchesByDay(LocalDateTime from, LocalDateTime to) {
        return countsPerDay("SELECT COUNT(*) FROM activity_logs WHERE event_type='SEARCH' AND timestamp >= ? AND timestamp < ?",
                from, to);
    }
    @Override @Transactional(readOnly = true)
    public List<Object[]> quotesByDay(LocalDateTime from, LocalDateTime to) {
        return countsPerDay("SELECT COUNT(*) FROM price_quotations WHERE created_at >= ? AND created_at < ?",
                from, to);
    }
    @Override @Transactional(readOnly = true)
    public List<Object[]> bookingsByDay(LocalDateTime from, LocalDateTime to) {
        return countsPerDay("SELECT COUNT(*) FROM service_requests WHERE created_at >= ? AND created_at < ?",
                from, to);
    }

    // ---------- UC-A04 extras ----------
    @Override
    @Transactional(readOnly = true)
    public List<Object[]> corridorCounts(LocalDateTime from, LocalDateTime to) {
        final String sql =
                "SELECT a1.city AS pickup_city, a2.city AS delivery_city, COUNT(*) AS trips " +
                        "FROM service_requests sr " +
                        "JOIN addresses a1 ON a1.address_id = sr.pickup_address_id " +
                        "JOIN addresses a2 ON a2.address_id = sr.delivery_address_id " +
                        "WHERE sr.created_at >= ? AND sr.created_at <= ? " +
                        "GROUP BY a1.city, a2.city " +
                        "ORDER BY trips DESC";
        Timestamp f = Timestamp.valueOf(from), t = Timestamp.valueOf(to);
        return jdbc.query(sql, (rs, rowNum) -> new Object[]{
                rs.getString("pickup_city"), rs.getString("delivery_city"), rs.getLong("trips")
        }, f, t);
    }

    @Override
    @Transactional(readOnly = true)
    public Object aovCompleted(LocalDateTime from, LocalDateTime to) {
        final String sql = "SELECT AVG(total_cost) AS aov FROM service_requests WHERE status='completed' AND created_at >= ? AND created_at <= ?";
        return jdbc.queryForObject(sql, (rs, rn) -> rs.getBigDecimal("aov"),
                Timestamp.valueOf(from), Timestamp.valueOf(to));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> cancelReasons(LocalDateTime from, LocalDateTime to) {
        final String sql =
                "SELECT COALESCE(event_description,'(unknown)') AS reason, COUNT(*) AS cnt " +
                        "FROM activity_logs WHERE event_type IN ('CANCEL','CANCELLED') AND timestamp >= ? AND timestamp <= ? " +
                        "GROUP BY COALESCE(event_description,'(unknown)') ORDER BY cnt DESC";
        Timestamp f = Timestamp.valueOf(from), t = Timestamp.valueOf(to);
        return jdbc.query(sql, (rs, rn) -> new Object[]{ rs.getString("reason"), rs.getLong("cnt") }, f, t);
    }

    // ---------- helper (vendor-neutral daily bucketing) ----------
    private List<Object[]> countsPerDay(String sql, LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = new ArrayList<>();
        LocalDate d = from.toLocalDate();
        LocalDate end = to.toLocalDate();
        while (!d.isAfter(end)) {
            Timestamp s = Timestamp.valueOf(d.atStartOfDay());
            Timestamp e = Timestamp.valueOf(d.plusDays(1).atStartOfDay());
            Long c = jdbc.queryForObject(sql, Long.class, s, e);
            rows.add(new Object[]{ java.sql.Date.valueOf(d), c == null ? 0L : c });
            d = d.plusDays(1);
        }
        return rows;
    }
}
