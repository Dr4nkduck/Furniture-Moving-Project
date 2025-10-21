package SWP301.Furniture_Moving_Project.repository;
import SWP301.Furniture_Moving_Project.model.FurnitureType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FurnitureTypeRepository extends JpaRepository<FurnitureType, Integer> {
    List<FurnitureType> findAllByOrderByNameAsc();
}