package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.model.ProviderServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderServiceItemRepository extends JpaRepository<ProviderServiceItem, Long> {
    List<ProviderServiceItem> findByProviderAndActiveTrueOrderByNameAsc(Provider provider);
}
