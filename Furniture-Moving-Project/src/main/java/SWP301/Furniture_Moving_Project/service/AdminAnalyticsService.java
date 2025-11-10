package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.CustomerTrendsDTO;
import SWP301.Furniture_Moving_Project.dto.CustomerTrendsDTO.CancelReasonRow;
import SWP301.Furniture_Moving_Project.dto.CustomerTrendsDTO.CorridorRow;
import SWP301.Furniture_Moving_Project.dto.CustomerTrendsDTO.FunnelPoint;
import SWP301.Furniture_Moving_Project.dto.ProviderStatsDTO;
import SWP301.Furniture_Moving_Project.repository.AdminAnalyticsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminAnalyticsService {

    private final AdminAnalyticsRepository repo;
    public AdminAnalyticsService(AdminAnalyticsRepository repo) { this.repo = repo; }

    private static final LocalDate DEFAULT_FROM = LocalDate.of(1970,1,1);
    private static final LocalDate DEFAULT_TO   = LocalDate.now();

    // ---------- UC-A03 ----------
    public List<ProviderStatsDTO> getProviderStats(LocalDate from, LocalDate to, String nameFilter) {
        LocalDate fromNz = (from == null) ? DEFAULT_FROM : from;
        LocalDate toNz   = (to   == null) ? DEFAULT_TO   : to;
        LocalDateTime f = fromNz.atStartOfDay();
        LocalDateTime t = toNz.plusDays(1).atStartOfDay().minusNanos(1);

        return repo.providerStats(f, t, nameFilter).stream().map(r -> new ProviderStatsDTO(
                (Integer) r[0], (String)  r[1],
                ((Number) r[2]).longValue(), ((Number) r[3]).longValue(), ((Number) r[4]).longValue(),
                (BigDecimal) r[5], (BigDecimal) r[6], (BigDecimal) r[7]
        )).collect(Collectors.toList());
    }

    // ---------- UC-A04 ----------
    public CustomerTrendsDTO getCustomerTrends(LocalDate from, LocalDate to, int top, String granularity) {
        LocalDate fromNz = (from == null) ? DEFAULT_FROM : from;
        LocalDate toNz   = (to   == null) ? DEFAULT_TO   : to;
        LocalDateTime f = fromNz.atStartOfDay();
        LocalDateTime t = toNz.plusDays(1).atStartOfDay().minusNanos(1);

        Map<LocalDate, long[]> day = new TreeMap<>();
        repo.searchesByDay(f, t).forEach(r -> day.computeIfAbsent(((java.sql.Date)r[0]).toLocalDate(), k -> new long[3])[0] = ((Number)r[1]).longValue());
        repo.quotesByDay(f, t).forEach(r -> day.computeIfAbsent(((java.sql.Date)r[0]).toLocalDate(), k -> new long[3])[1] = ((Number)r[1]).longValue());
        repo.bookingsByDay(f, t).forEach(r -> day.computeIfAbsent(((java.sql.Date)r[0]).toLocalDate(), k -> new long[3])[2] = ((Number)r[1]).longValue());

        List<FunnelPoint> funnel;
        String g = (granularity == null) ? "month" : granularity.trim().toLowerCase();
        if ("year".equals(g)) {
            Map<Year, long[]> byYear = new TreeMap<>();
            day.forEach((dte, arr) -> {
                Year y = Year.of(dte.getYear());
                byYear.computeIfAbsent(y, k -> new long[3]);
                byYear.get(y)[0] += arr[0]; byYear.get(y)[1] += arr[1]; byYear.get(y)[2] += arr[2];
            });
            funnel = byYear.entrySet().stream()
                    .map(e -> new FunnelPoint(LocalDate.of(e.getKey().getValue(),1,1), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                    .collect(Collectors.toList());
        } else {
            Map<YearMonth, long[]> byMonth = new TreeMap<>();
            day.forEach((dte, arr) -> {
                YearMonth ym = YearMonth.of(dte.getYear(), dte.getMonth());
                byMonth.computeIfAbsent(ym, k -> new long[3]);
                byMonth.get(ym)[0] += arr[0]; byMonth.get(ym)[1] += arr[1]; byMonth.get(ym)[2] += arr[2];
            });
            funnel = byMonth.entrySet().stream()
                    .map(e -> new FunnelPoint(LocalDate.of(e.getKey().getYear(), e.getKey().getMonth(), 1), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                    .collect(Collectors.toList());
        }

        List<CorridorRow> corridors = repo.corridorCounts(f, t).stream()
                .map(r -> new CorridorRow((String) r[0], (String) r[1], ((Number) r[2]).longValue()))
                .collect(Collectors.toList());
        List<CorridorRow> topCorridors = corridors.stream().limit(Math.max(1, top)).collect(Collectors.toList());

        BigDecimal aov = Optional.ofNullable((BigDecimal) repo.aovCompleted(f, t)).orElse(BigDecimal.ZERO);

        List<CancelReasonRow> reasons = repo.cancelReasons(f, t).stream()
                .map(r -> new CancelReasonRow((String) r[0], ((Number) r[1]).longValue()))
                .collect(Collectors.toList());

        return new CustomerTrendsDTO(funnel, topCorridors, aov, reasons);
    }

    // ---------- CSV helpers ----------
    public String toProviderStatsCsv(List<ProviderStatsDTO> rows) {
        List<String[]> csv = new ArrayList<>();
        csv.add(new String[]{"provider_id","company_name","jobs_total","jobs_completed","cancellations","revenue_total","acceptance_percent","avg_rating"});
        for (ProviderStatsDTO r : rows) {
            csv.add(new String[]{
                    String.valueOf(r.getProviderId()), nz(r.getCompanyName()),
                    String.valueOf(r.getJobsTotal()), String.valueOf(r.getJobsCompleted()),
                    String.valueOf(r.getCancellations()), dec(r.getRevenueTotal()),
                    dec(r.getAcceptancePercent()), dec(r.getAvgRating())
            });
        }
        return lines(csv);
    }

    public String toFunnelCsv(List<FunnelPoint> f) {
        List<String[]> csv = new ArrayList<>();
        csv.add(new String[]{"date","searches","quotes","bookings"});
        for (FunnelPoint p : f) {
            csv.add(new String[]{p.getDate().toString(), String.valueOf(p.getSearches()),
                    String.valueOf(p.getQuotes()), String.valueOf(p.getBookings())});
        }
        return lines(csv);
    }

    public String toCorridorsCsv(List<CorridorRow> c) {
        List<String[]> csv = new ArrayList<>();
        csv.add(new String[]{"pickup_city","delivery_city","trips"});
        for (CorridorRow r : c) csv.add(new String[]{nz(r.getPickupCity()), nz(r.getDeliveryCity()), String.valueOf(r.getTrips())});
        return lines(csv);
    }

    public String toCancelReasonsCsv(List<CancelReasonRow> rows) {
        List<String[]> csv = new ArrayList<>();
        csv.add(new String[]{"reason","count"});
        for (CancelReasonRow r : rows) csv.add(new String[]{nz(r.getReason()), String.valueOf(r.getCount())});
        return lines(csv);
    }

    private static String nz(String s){ return s==null? "": s; }
    private static String dec(BigDecimal b){ return b==null? "": b.toPlainString(); }
    private static String esc(String s){
        if (s==null) return "";
        boolean q = s.contains(",") || s.contains("\"") || s.contains("\n");
        String v = s.replace("\"","\"\"");
        return q ? "\""+v+"\"" : v;
    }
    private static String lines(List<String[]> rows){
        return rows.stream().map(r -> String.join(",", Arrays.stream(r).map(AdminAnalyticsService::esc).toArray(String[]::new))).collect(Collectors.joining("\n"));
    }
}
