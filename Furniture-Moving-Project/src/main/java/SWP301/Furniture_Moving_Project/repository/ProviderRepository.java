package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Integer> {

    // Tuỳ mapping Provider -> User của bạn (user.username).
    @Query("select p.providerId from Provider p where p.userId = :username")
    Optional<Integer> findProviderIdByUsername(String username);
}
