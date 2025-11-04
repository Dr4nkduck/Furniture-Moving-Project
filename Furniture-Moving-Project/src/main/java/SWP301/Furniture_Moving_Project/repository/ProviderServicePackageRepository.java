// repository/ProviderServicePackageRepository.java
package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ProviderServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderServicePackageRepository extends JpaRepository<ProviderServicePackage, Integer> {

    Optional<ProviderServicePackage> findByProvider_ProviderIdAndServicePackage_PackageId(Integer providerId, Integer packageId);

    List<ProviderServicePackage> findByProvider_ProviderId(Integer providerId);
}
