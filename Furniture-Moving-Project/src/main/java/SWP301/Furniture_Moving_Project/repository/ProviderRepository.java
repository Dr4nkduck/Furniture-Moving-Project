// repository/ProviderRepository.java
package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {

    @Query(value = """
            SELECT sp.provider_id
              FROM service_providers sp
              JOIN users u ON u.user_id = sp.user_id
             WHERE u.username = :username
            """, nativeQuery = true)
    Optional<Integer> findProviderIdByUsername(@Param("username") String username);
}
