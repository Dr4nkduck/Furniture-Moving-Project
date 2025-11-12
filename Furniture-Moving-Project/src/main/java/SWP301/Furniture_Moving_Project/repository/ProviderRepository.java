// repository/ProviderRepository.java
package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Integer> {
    // Provider.user.username -> providerId
//    @Query("select p.providerId from Provider p where p.user.username = :username")
    Optional<Provider> findByUser_Username(String username);
}
