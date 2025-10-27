package SWP301.Furniture_Moving_Project.repository;
import SWP301.Furniture_Moving_Project.model.ProviderServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ProviderServicePackageRepository extends JpaRepository<ProviderServicePackage, Integer> {
    Optional<ProviderServicePackage> findByProviderIdAndServicePackage_PackageId(Integer providerId, Integer packageId);
}