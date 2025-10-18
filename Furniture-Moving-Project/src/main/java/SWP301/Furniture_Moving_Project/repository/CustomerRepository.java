package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserId(Long userId);
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByEmail(String email);
}
