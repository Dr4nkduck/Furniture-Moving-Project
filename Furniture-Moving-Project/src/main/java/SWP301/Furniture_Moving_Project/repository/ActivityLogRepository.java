package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ActivityLog;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Integer> {

    @Query("""
        SELECT a FROM ActivityLog a
        WHERE (:q IS NULL OR :q = '' OR
               LOWER(a.eventType) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(a.eventDescription) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<ActivityLog> search(String q, Pageable pageable);
}
