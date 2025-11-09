package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Customer;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    @Query(value = """
        SELECT c.customer_id
        FROM customers c
        JOIN users u ON u.user_id = c.user_id
        WHERE u.username = :username
        """, nativeQuery = true)
    Optional<Integer> findCustomerIdByUsername(@Param("username") String username);
}

