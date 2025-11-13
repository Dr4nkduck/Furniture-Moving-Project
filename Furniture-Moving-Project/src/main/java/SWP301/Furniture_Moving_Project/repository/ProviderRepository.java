package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Integer> {

    // Provider.user.username -> providerId
    @Query("select p.providerId from Provider p where p.user.username = :username")
    Optional<Integer> findProviderIdByUsername(String username);

    List<Provider> findByVerificationStatusOrderByCompanyNameAsc(String verificationStatus);

    List<Provider> findByCompanyNameContainingIgnoreCase(String companyName);

    // Query nhẹ cho dropdown /request (không phụ thuộc entity)
    @Query(value = """
        SELECT 
          p.provider_id  AS providerId,
          p.company_name AS companyName,
          CAST(p.rating AS float) AS rating
        FROM providers p
        ORDER BY p.company_name
        """, nativeQuery = true)
    List<Map<String,Object>> findAvailableProvidersLight();
}
