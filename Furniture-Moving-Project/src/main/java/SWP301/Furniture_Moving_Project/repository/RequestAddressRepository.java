package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.RequestAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RequestAddressRepository extends JpaRepository<RequestAddress, Integer> {
    
    List<RequestAddress> findByServiceRequestRequestId(Integer requestId);
    
    List<RequestAddress> findByServiceRequestRequestIdAndAddressType(Integer requestId, String addressType);
}