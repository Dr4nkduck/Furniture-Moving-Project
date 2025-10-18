package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    @Query("""
           select sr
           from ServiceRequest sr
           where sr.provider.providerId = :providerId
             and sr.status in :statuses
           order by sr.scheduledAt desc
           """)
    List<ServiceRequest> findByProviderIdAndStatusIn(
            @Param("providerId") Long providerId,
            @Param("statuses") Collection<OrderStatus> statuses
    );

    Optional<ServiceRequest> findByIdAndProviderId(Long id, Long providerId);

    long countByProviderIdAndStatus(Long providerId, OrderStatus status);

    List<ServiceRequest> findByProviderIdAndScheduledAtBetweenOrderByScheduledAtDesc(
            Long providerId, LocalDateTime start, LocalDateTime end
    );

    List<ServiceRequest> findByProviderIdAndStatusInOrderByScheduledAtDesc(
            Long providerId, Collection<OrderStatus> statuses
    );
}
