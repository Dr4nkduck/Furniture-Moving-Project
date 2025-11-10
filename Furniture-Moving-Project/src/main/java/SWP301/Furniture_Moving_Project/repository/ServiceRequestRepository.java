package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {

    List<ServiceRequest> findByCustomerId(Integer customerId);

    List<ServiceRequest> findByStatus(String status);

    List<ServiceRequest> findByCustomerIdAndStatus(Integer customerId, String status);

    List<ServiceRequest> findTop5ByCustomerIdOrderByCreatedAtDesc(Integer customerId);
    // Nếu entity không có 'createdAt', đổi sang:
    // List<ServiceRequest> findTop5ByCustomerIdOrderByRequestDateDesc(Integer customerId);

    long countByProviderId(Integer providerId);

    long countByProviderIdAndStatus(Integer providerId, String status);

    @Query("""
           select sr
           from ServiceRequest sr
           left join fetch sr.pickupAddress
           left join fetch sr.deliveryAddress
           where sr.requestId = :id
           """)
    Optional<ServiceRequest> findWithAddressesById(@Param("id") Integer id);
}
