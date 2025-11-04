// repository/ProviderPackageFurniturePriceRepository.java
package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ProviderPackageFurniturePrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderPackageFurniturePriceRepository extends JpaRepository<ProviderPackageFurniturePrice, Integer> {

    List<ProviderPackageFurniturePrice> findByProvider_ProviderIdAndServicePackage_PackageId(Integer providerId, Integer packageId);

    Optional<ProviderPackageFurniturePrice> findByProvider_ProviderIdAndServicePackage_PackageIdAndFurnitureType_FurnitureTypeId(
            Integer providerId, Integer packageId, Integer furnitureTypeId
    );
}
