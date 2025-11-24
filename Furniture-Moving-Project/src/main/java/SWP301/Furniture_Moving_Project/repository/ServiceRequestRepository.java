// src/main/java/SWP301/Furniture_Moving_Project/repository/ServiceRequestRepository.java
package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderItemProjection;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderDetailProjection;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {

    List<ServiceRequest> findByCustomerId(Integer customerId);
    List<ServiceRequest> findByStatus(String status);
    List<ServiceRequest> findByCustomerIdAndStatus(Integer customerId, String status);
    List<ServiceRequest> findTop5ByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    long countByProviderId(Integer providerId);
    long countByProviderIdAndStatus(Integer providerId, String status);

    long countByProviderIdAndPreferredDateAndStatusIn(Integer providerId, LocalDate preferredDate, Collection<String> status);

    @Query(value = """
        SELECT 
            sr.request_id      AS requestId,
            sr.status          AS status,
            sr.request_date    AS requestDate,
            sr.preferred_date  AS preferredDate,
            u.first_name       AS customerFirstName,
            u.last_name        AS customerLastName,
            pu.street_address  AS pickupStreet,
            pu.city            AS pickupCity,
            de.street_address  AS deliveryStreet,
            de.city            AS deliveryCity,
            sr.total_cost      AS totalCost
        FROM service_requests sr
        JOIN customers c   ON c.customer_id = sr.customer_id
        JOIN users     u   ON u.user_id     = c.user_id
        JOIN addresses pu  ON pu.address_id = sr.pickup_address_id
        JOIN addresses de  ON de.address_id = sr.delivery_address_id
        WHERE sr.provider_id = :providerId
          AND (:status IS NULL OR sr.status = :status)
          AND (
               :q IS NULL
            OR  u.first_name LIKE CONCAT('%', :q, '%')
            OR  u.last_name  LIKE CONCAT('%', :q, '%')
            OR  CONVERT(VARCHAR(20), sr.request_id) LIKE CONCAT('%', :q, '%')
            OR  pu.street_address LIKE CONCAT('%', :q, '%')
            OR  de.street_address LIKE CONCAT('%', :q, '%')
          )
        ORDER BY sr.request_date DESC
        """, nativeQuery = true)
    List<ProviderOrderSummaryProjection> findProviderOrders(
            @Param("providerId") Integer providerId,
            @Param("status") String status,
            @Param("q") String q
    );

    @Query(value = """
        SELECT 
            sr.request_id     AS requestId,
            sr.status         AS status,
            sr.request_date   AS requestDate,
            sr.preferred_date AS preferredDate,
            sr.total_cost     AS totalCost,
            u.first_name      AS customerFirstName,
            u.last_name       AS customerLastName,
            u.phone           AS customerPhone,
            u.email           AS customerEmail,
            pu.street_address AS pickupStreet,
            pu.city           AS pickupCity,
            pu.state          AS pickupState,
            pu.zip_code       AS pickupZip,
            de.street_address AS deliveryStreet,
            de.city           AS deliveryCity,
            de.state          AS deliveryState,
            de.zip_code       AS deliveryZip
        FROM service_requests sr
        JOIN customers c   ON c.customer_id = sr.customer_id
        JOIN users     u   ON u.user_id     = c.user_id
        JOIN addresses pu  ON pu.address_id = sr.pickup_address_id
        JOIN addresses de  ON de.address_id = sr.delivery_address_id
        WHERE sr.request_id = :requestId AND sr.provider_id = :providerId
        """, nativeQuery = true)
    ProviderOrderDetailProjection findOrderDetail(
            @Param("providerId") Integer providerId,
            @Param("requestId") Integer requestId
    );

    @Query(value = """
        SELECT 
            fi.item_id    AS itemId,
            fi.item_type  AS itemType,
            fi.size       AS size,
            fi.quantity   AS quantity,
            fi.is_fragile AS isFragile
        FROM furniture_items fi
        WHERE fi.request_id = :requestId
        ORDER BY fi.item_id
        """, nativeQuery = true)
    List<ProviderOrderItemProjection> findOrderItems(@Param("requestId") Integer requestId);

    @Modifying
    @Query(value = """
        UPDATE service_requests 
        SET status = :status,
            cancel_reason = :cancelReason
        WHERE request_id = :requestId AND provider_id = :providerId
        """, nativeQuery = true)
    int providerUpdateStatus(
            @Param("providerId") Integer providerId,
            @Param("requestId") Integer requestId,
            @Param("status") String status,
            @Param("cancelReason") String cancelReason
    );

    /* ==== PAYMENT GUARD: chỉ cho phép khi đơn thuộc đúng user & đã có provider nhận ==== */
    @Query(value = """
        SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END
        FROM dbo.service_requests sr
        JOIN dbo.customers c ON c.customer_id = sr.customer_id
        JOIN dbo.users u ON u.user_id = c.user_id
        WHERE sr.request_id = :requestId
          AND u.username   = :username
          AND sr.provider_id IS NOT NULL
          -- Nếu muốn siết theo trạng thái thanh toán được phép, bỏ comment dòng dưới:
          -- AND sr.status IN ('ASSIGNED','ACCEPTED','IN_PROGRESS','COMPLETED')
        """, nativeQuery = true)
    int canAccessPayment(@Param("requestId") Integer requestId,
                         @Param("username")  String username);
}
