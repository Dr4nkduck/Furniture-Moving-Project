package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderDetailView;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderItemRow;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderListView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProviderOrderViewRepository extends JpaRepository<SWP301.Furniture_Moving_Project.model.ServiceRequest, Integer> {

    @Query(
            value = """
        SELECT sr.request_id AS requestId,
               CONCAT(u.first_name, ' ', u.last_name) AS customerName,
               sr.preferred_date AS preferredDate,
               CONCAT(a1.street_address, ', ', a1.city, ', ', a1.state) AS pickupAddress,
               CONCAT(a2.street_address, ', ', a2.city, ', ', a2.state) AS deliveryAddress,
               sr.status AS status,
               COALESCE(sr.total_cost, 0) AS totalCost
        FROM service_requests sr
        JOIN customers c   ON c.customer_id = sr.customer_id
        JOIN users u       ON u.user_id     = c.user_id
        JOIN addresses a1  ON a1.address_id = sr.pickup_address_id
        JOIN addresses a2  ON a2.address_id = sr.delivery_address_id
        WHERE sr.provider_id = :providerId
          AND (:status IS NULL OR sr.status = :status)
          AND (
               :q IS NULL OR :q = '' OR
               u.first_name LIKE CONCAT('%', :q, '%') OR
               u.last_name  LIKE CONCAT('%', :q, '%')  OR
               a1.street_address LIKE CONCAT('%', :q, '%') OR
               a2.street_address LIKE CONCAT('%', :q, '%')
          )
        ORDER BY sr.request_date DESC
      """,
            countQuery = """
        SELECT COUNT(1)
        FROM service_requests sr
        JOIN customers c   ON c.customer_id = sr.customer_id
        JOIN users u       ON u.user_id     = c.user_id
        JOIN addresses a1  ON a1.address_id = sr.pickup_address_id
        JOIN addresses a2  ON a2.address_id = sr.delivery_address_id
        WHERE sr.provider_id = :providerId
          AND (:status IS NULL OR sr.status = :status)
          AND (
               :q IS NULL OR :q = '' OR
               u.first_name LIKE CONCAT('%', :q, '%') OR
               u.last_name  LIKE CONCAT('%', :q, '%')  OR
               a1.street_address LIKE CONCAT('%', :q, '%') OR
               a2.street_address LIKE CONCAT('%', :q, '%')
          )
      """,
            nativeQuery = true)
    Page<ProviderOrderListView> pageOrders(
            @Param("providerId") Integer providerId,
            @Param("status") String status,
            @Param("q") String q,
            Pageable pageable
    );

    @Query(value = """
        SELECT TOP 1
               sr.request_id AS requestId,
               CONCAT(u.first_name, ' ', u.last_name) AS customerName,
               u.phone AS customerPhone,
               sr.preferred_date AS preferredDate,
               sr.request_date AS requestDate,
               CONCAT(a1.street_address, ', ', a1.city, ', ', a1.state) AS pickupAddress,
               CONCAT(a2.street_address, ', ', a2.city, ', ', a2.state) AS deliveryAddress,
               sr.status AS status,
               COALESCE(sr.total_cost, 0) AS totalCost
        FROM service_requests sr
        JOIN customers c   ON c.customer_id = sr.customer_id
        JOIN users u       ON u.user_id     = c.user_id
        JOIN addresses a1  ON a1.address_id = sr.pickup_address_id
        JOIN addresses a2  ON a2.address_id = sr.delivery_address_id
        WHERE sr.request_id = :requestId AND sr.provider_id = :providerId
      """, nativeQuery = true)
    ProviderOrderDetailView getOrderDetail(
            @Param("providerId") Integer providerId,
            @Param("requestId") Integer requestId
    );

    @Query(value = """
        SELECT item_id AS itemId, item_type AS itemType, size, quantity, is_fragile AS isFragile
        FROM furniture_items
        WHERE request_id = :requestId
        ORDER BY item_id
      """, nativeQuery = true)
    List<ProviderOrderItemRow> getItems(@Param("requestId") Integer requestId);

    @Modifying
    @Query(value = """
        UPDATE service_requests
        SET status = :status
        WHERE request_id = :requestId AND provider_id = :providerId
      """, nativeQuery = true)
    int updateStatus(@Param("providerId") Integer providerId,
                     @Param("requestId") Integer requestId,
                     @Param("status") String status);
}
