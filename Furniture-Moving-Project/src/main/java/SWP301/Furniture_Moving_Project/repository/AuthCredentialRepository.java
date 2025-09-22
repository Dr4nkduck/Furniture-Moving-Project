package SWP301.Furniture_Moving_Project.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import SWP301.Furniture_Moving_Project.model.AuthCredential;

@Repository
public interface AuthCredentialRepository extends JpaRepository<AuthCredential, Integer> {
    Optional<AuthCredential> findByUserId(Integer userId);
}
