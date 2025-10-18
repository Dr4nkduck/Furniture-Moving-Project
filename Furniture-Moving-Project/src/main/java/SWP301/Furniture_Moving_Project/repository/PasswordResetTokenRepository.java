package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.PasswordResetToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    @Query("SELECT t FROM PasswordResetToken t " +
           "WHERE t.userId = :userId AND t.consumed = false AND t.expiresAt > :now " +
           "ORDER BY t.createdAt DESC")
    Optional<PasswordResetToken> findActiveByUserId(Integer userId, OffsetDateTime now);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.consumed = true WHERE t.userId = :userId AND t.consumed = false")
    int consumeAllForUser(Integer userId);
}
