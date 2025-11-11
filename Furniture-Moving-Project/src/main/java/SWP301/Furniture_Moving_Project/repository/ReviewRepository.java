package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByProviderIdOrderByCreatedAtDesc(Integer providerId);

}
