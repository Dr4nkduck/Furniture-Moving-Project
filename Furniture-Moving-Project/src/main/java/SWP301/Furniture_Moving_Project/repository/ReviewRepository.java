package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Dùng cho provider: xem list đánh giá theo provider
    List<Review> findByProviderIdOrderByCreatedAtDesc(Integer providerId);

    // Dùng cho customer request detail: 1 request <-> 1 review
    Optional<Review> findByRequestId(Integer requestId);

    boolean existsByRequestId(Integer requestId);
}
