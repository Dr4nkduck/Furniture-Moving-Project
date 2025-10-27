package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.UserRateLimitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface UserRateLimitLogRepository extends JpaRepository<UserRateLimitLog, Long> {

    @Query("SELECT COUNT(l) FROM UserRateLimitLog l WHERE l.userId = :userId AND l.requestTimestamp >= :since")
    long countByUserIdAndRequestTimestampAfter(@Param("userId") Integer userId, @Param("since") OffsetDateTime since);

    List<UserRateLimitLog> findByUserIdAndRequestTimestampAfterOrderByRequestTimestampDesc(Integer userId, OffsetDateTime since);

    void deleteByRequestTimestampBefore(OffsetDateTime before);
}
