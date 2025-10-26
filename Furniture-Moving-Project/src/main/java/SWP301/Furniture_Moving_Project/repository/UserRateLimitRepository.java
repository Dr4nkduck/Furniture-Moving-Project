package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.UserRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRateLimitRepository extends JpaRepository<UserRateLimit, Integer> {
    Optional<UserRateLimit> findByUserIdAndStatus(Integer userId, String status);
    Optional<UserRateLimit> findByUserId(Integer userId);
}
