package SWP301.Furniture_Moving_Project.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminAnalyticsRepository {
    // UC-A03
    List<Object[]> providerStats(LocalDateTime from, LocalDateTime to, String nameLike);

    // UC-A04 (daily buckets; service rolls up to month/year)
    List<Object[]> searchesByDay(LocalDateTime from, LocalDateTime to);
    List<Object[]> quotesByDay(LocalDateTime from, LocalDateTime to);
    List<Object[]> bookingsByDay(LocalDateTime from, LocalDateTime to);

    // UC-A04 extras
    List<Object[]> corridorCounts(LocalDateTime from, LocalDateTime to);
    Object aovCompleted(LocalDateTime from, LocalDateTime to);
    List<Object[]> cancelReasons(LocalDateTime from, LocalDateTime to);
}
