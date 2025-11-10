package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Payment;
import SWP301.Furniture_Moving_Project.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findTopByServiceRequestIdOrderByCreatedAtDesc(Integer serviceRequestId);
    Optional<Payment> findTopByServiceRequestIdAndStatusOrderByCreatedAtDesc(Integer serviceRequestId, PaymentStatus status);
}
