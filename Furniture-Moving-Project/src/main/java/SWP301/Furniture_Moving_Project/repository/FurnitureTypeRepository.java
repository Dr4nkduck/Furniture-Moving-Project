// repository/FurnitureTypeRepository.java
package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.FurnitureType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FurnitureTypeRepository extends JpaRepository<FurnitureType, Integer> {
    Optional<FurnitureType> findByNameIgnoreCase(String name);
    List<FurnitureType> findAllByOrderByNameAsc();
}
