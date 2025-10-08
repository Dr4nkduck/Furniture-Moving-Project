package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FurnitureItemRepository extends JpaRepository<FurnitureItem, Integer> {
    
    List<FurnitureItem> findByServiceRequestRequestId(Integer requestId);
}