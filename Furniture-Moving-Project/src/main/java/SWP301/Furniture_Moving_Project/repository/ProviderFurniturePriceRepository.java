package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.model.ProviderFurniturePrice;
import SWP301.Furniture_Moving_Project.model.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderFurniturePriceRepository extends JpaRepository<ProviderFurniturePrice, Integer> {
    List<ProviderFurniturePrice> findByProviderAndServicePackage(Provider provider, ServicePackage servicePackage);

    Optional<ProviderFurniturePrice> findByProviderAndServicePackageAndFurnitureItem(
            Provider provider, ServicePackage servicePackage, FurnitureItem furnitureItem
    );

    void deleteByProviderAndServicePackageAndFurnitureItem(
            Provider provider, ServicePackage servicePackage, FurnitureItem furnitureItem
    );
}
