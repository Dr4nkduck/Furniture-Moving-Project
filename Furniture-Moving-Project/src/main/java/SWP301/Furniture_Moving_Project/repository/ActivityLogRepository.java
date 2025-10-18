package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Integer> {

    @Query("""
        SELECT a FROM ActivityLog a
        WHERE (:q IS NULL OR :q = '' OR
               LOWER(a.eventType)        LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(a.eventDescription) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(a.ipAddress)        LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<ActivityLog> search(@Param("q") String q, Pageable pageable);
}
