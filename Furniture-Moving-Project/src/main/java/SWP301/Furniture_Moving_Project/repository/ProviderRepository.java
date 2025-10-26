package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.dto.ProviderDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProviderRepository extends JpaRepository<Provider, Integer> {

    @Query("""
           select new SWP301.Furniture_Moving_Project.dto.ProviderDTO(
             p.providerId, p.companyName, p.rating
           )
           from Provider p
           where p.verificationStatus = 'verified'
           order by p.companyName asc
           """)
    List<ProviderDTO> findAvailableProviders();
}
