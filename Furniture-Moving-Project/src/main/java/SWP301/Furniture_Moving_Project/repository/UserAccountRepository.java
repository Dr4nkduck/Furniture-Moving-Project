package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    // ✅ keep your current derived search (no dates)
    Page<UserAccount> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String username, String email, String firstName, String lastName, Pageable pageable
    );

    // ✅ used by login loader
    Optional<UserAccount> findByUsernameOrEmail(String username, String email);

    // ➕ NEW: search with optional createdFrom/createdTo (for the UI filter)
    @Query("""
            SELECT u FROM UserAccount u
            WHERE (:q IS NULL OR :q = '' OR
                   LOWER(u.username)  LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(u.email)     LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:from IS NULL OR u.createdAt >= :from)
              AND (:to   IS NULL OR u.createdAt <  :to)
            """)
    Page<UserAccount> searchAdmin(@Param("q") String q,
                                  @Param("from") OffsetDateTime createdFrom,
                                  @Param("to") OffsetDateTime createdTo,
                                  Pageable pageable);

    /**
     * Case-insensitive login by either username or email
     */
    @Query("""
                SELECT u FROM UserAccount u
                WHERE LOWER(u.username) = LOWER(:login)
                   OR LOWER(u.email)    = LOWER(:login)
            """)
    Optional<UserAccount> findByLoginIgnoreCase(@Param("login") String login);
}
