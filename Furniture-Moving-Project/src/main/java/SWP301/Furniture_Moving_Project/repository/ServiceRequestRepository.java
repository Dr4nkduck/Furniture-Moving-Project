package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {
    
    List<ServiceRequest> findByCustomerId(Integer customerId);
    
    List<ServiceRequest> findByStatus(String status);
    
    List<ServiceRequest> findByCustomerIdAndStatus(Integer customerId, String status);

    long countByProviderId(Integer providerId);

    long countByProviderIdAndStatus(Integer providerId, String status);

    // Danh sách theo provider + lọc status + tìm kiếm q (theo tên KH hoặc địa chỉ)
    @Query("""
        SELECT sr FROM ServiceRequest sr
        JOIN sr.customerId c
        JOIN sr.pickupAddress pa
        JOIN sr.deliveryAddress da
        WHERE sr.providerId = :providerId
          AND (:statuses IS NULL OR sr.status IN :statuses)
          AND (
               :q IS NULL OR :q = '' OR
               LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(pa.streetAddress) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(da.streetAddress) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY sr.preferredDate DESC, sr.requestDate DESC
    """)
    List<ServiceRequest> searchForProvider(
            @Param("providerId") Integer providerId,
            @Param("statuses") Collection<String> statuses,
            @Param("q") String q
    );

    // Load detail + fetch addresses & user
    @Query("""
        SELECT sr FROM ServiceRequest sr
        JOIN FETCH sr.customerId c
        JOIN FETCH sr.pickupAddress pa
        JOIN FETCH sr.deliveryAddress da
        WHERE sr.requestId = :requestId AND sr.providerId = :providerId
    """)
    Optional<ServiceRequest> findDetailForProvider(@Param("providerId") Integer providerId,
                                                   @Param("requestId") Integer requestId);

}