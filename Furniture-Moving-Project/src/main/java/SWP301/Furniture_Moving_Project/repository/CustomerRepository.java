package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {}
