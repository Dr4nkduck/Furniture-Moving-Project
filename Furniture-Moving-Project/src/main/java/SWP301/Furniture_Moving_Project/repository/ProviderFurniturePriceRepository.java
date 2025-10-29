package SWP301.Furniture_Moving_Project.repository;
import SWP301.Furniture_Moving_Project.model.ProviderFurniturePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProviderFurniturePriceRepository extends JpaRepository<ProviderFurniturePrice, Integer> {
    List<ProviderFurniturePrice> findByProviderId(Integer providerId);
    Optional<ProviderFurniturePrice> findByProviderIdAndFurnitureType_FurnitureTypeId(Integer providerId, Integer furnitureTypeId);
}