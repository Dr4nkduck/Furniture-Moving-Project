package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {
    
    List<ServiceRequest> findByCustomerId(Integer customerId);
    
    List<ServiceRequest> findByStatus(String status);
    
    List<ServiceRequest> findByCustomerIdAndStatus(Integer customerId, String status);
    
    List<ServiceRequest> findTop5ByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    long countByProviderId(Integer providerId);

    long countByProviderIdAndStatus(Integer providerId, String status);
}