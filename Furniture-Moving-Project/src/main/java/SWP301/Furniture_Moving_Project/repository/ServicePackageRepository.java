package SWP301.Furniture_Moving_Project.repository;
import SWP301.Furniture_Moving_Project.model.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Integer> {
    List<ServicePackage> findByActiveTrueOrderByNameAsc();
}