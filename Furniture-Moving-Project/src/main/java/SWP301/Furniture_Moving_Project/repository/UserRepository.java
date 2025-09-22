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
}
