package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    // handy for listing a customer's saved addresses
    List<Address> findByUserId(Long userId);
}
