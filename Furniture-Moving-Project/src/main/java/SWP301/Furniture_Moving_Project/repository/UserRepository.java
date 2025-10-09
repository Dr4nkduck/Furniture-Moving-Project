package SWP301.Furniture_Moving_Project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import SWP301.Furniture_Moving_Project.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    @Query(value = """
        SELECT r.role_name
        FROM roles r
        JOIN user_roles ur ON ur.role_id = r.role_id
        WHERE ur.user_id = :userId
        """, nativeQuery = true)
    List<String> findRoleNamesByUserId(Integer userId);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    /* ========= Admins (ADMIN + SUPER_ADMIN) =========
       LƯU Ý: Đây là native query nên tự ORDER BY theo cột DB (created_at).
       Không dùng Sort từ Pageable để tránh sinh "u.createdAt".
    */
    @Query(value = """
        SELECT DISTINCT u.*
        FROM users u
        JOIN user_roles ur ON ur.user_id = u.user_id
        JOIN roles r ON r.role_id = ur.role_id
        WHERE r.role_name IN ('ADMIN','SUPER_ADMIN')
        ORDER BY u.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT u.user_id)
        FROM users u
        JOIN user_roles ur ON ur.user_id = u.user_id
        JOIN roles r ON r.role_id = ur.role_id
        WHERE r.role_name IN ('ADMIN','SUPER_ADMIN')
        """,
        nativeQuery = true)
    org.springframework.data.domain.Page<User> findAdmins(org.springframework.data.domain.Pageable pageable);

    @Query(value = """
        SELECT COUNT(DISTINCT u.user_id)
        FROM users u
        JOIN user_roles ur ON ur.user_id = u.user_id
        JOIN roles r ON r.role_id = ur.role_id
        WHERE r.role_name IN ('ADMIN','SUPER_ADMIN')
        """, nativeQuery = true)
    long countAdmins();
}
