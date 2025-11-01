package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.model.ProviderServicePackage;
import SWP301.Furniture_Moving_Project.model.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderServicePackageRepository extends JpaRepository<ProviderServicePackage, Integer> {
    Optional<ProviderServicePackage> findByProviderAndServicePackage(Provider provider, ServicePackage servicePackage);
    List<ProviderServicePackage> findByProvider(Provider provider);
    boolean existsByProviderAndServicePackage(Provider provider, ServicePackage servicePackage);
}
