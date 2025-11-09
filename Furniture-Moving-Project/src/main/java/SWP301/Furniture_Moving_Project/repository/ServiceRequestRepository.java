package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.projection.InvoicePageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {

    // ======= Finder cơ bản =======
    List<ServiceRequest> findByCustomerId(Integer customerId);
    List<ServiceRequest> findByStatus(String status);
    List<ServiceRequest> findByCustomerIdAndStatus(Integer customerId, String status);

    // ======= View hoá đơn theo request_id (ADMIN xem được mọi đơn) =======
    @Query(value = """
      SELECT 
        sr.request_id AS invoiceId,
        CONVERT(VARCHAR(10), sr.request_date, 105) AS invoiceDate,
        sr.status AS status,
        CAST(sr.total_cost AS VARCHAR(50)) AS totalCost,
        sp.company_name AS providerCompany,
        CONCAT(a1.street_address, ', ', a1.city, ', ', a1.state) AS pickupAddress,
        CONCAT(a2.street_address, ', ', a2.city, ', ', a2.state) AS deliveryAddress,
        (
          SELECT fi.item_id, fi.item_type AS item_name, fi.size, fi.quantity, fi.is_fragile, fi.special_handling
          FROM furniture_items fi
          WHERE fi.request_id = sr.request_id
          FOR JSON PATH
        ) AS furnitureItemsJson
      FROM service_requests sr
      JOIN addresses a1 ON a1.address_id = sr.pickup_address_id
      JOIN addresses a2 ON a2.address_id = sr.delivery_address_id
      LEFT JOIN service_providers sp ON sp.provider_id = sr.provider_id
      WHERE sr.request_id = :requestId
      """, nativeQuery = true)
    Optional<InvoicePageView> findInvoiceById(@Param("requestId") Integer requestId);

    // ======= View hoá đơn ràng buộc theo provider (PROVIDER chỉ xem đơn của mình) =======
    @Query(value = """
      SELECT 
        sr.request_id AS invoiceId,
        CONVERT(VARCHAR(10), sr.request_date, 105) AS invoiceDate,
        sr.status AS status,
        CAST(sr.total_cost AS VARCHAR(50)) AS totalCost,
        sp.company_name AS providerCompany,
        CONCAT(a1.street_address, ', ', a1.city, ', ', a1.state) AS pickupAddress,
        CONCAT(a2.street_address, ', ', a2.city, ', ', a2.state) AS deliveryAddress,
        (
          SELECT fi.item_id, fi.item_type AS item_name, fi.size, fi.quantity, fi.is_fragile, fi.special_handling
          FROM furniture_items fi
          WHERE fi.request_id = sr.request_id
          FOR JSON PATH
        ) AS furnitureItemsJson
      FROM service_requests sr
      JOIN addresses a1 ON a1.address_id = sr.pickup_address_id
      JOIN addresses a2 ON a2.address_id = sr.delivery_address_id
      LEFT JOIN service_providers sp ON sp.provider_id = sr.provider_id
      WHERE sr.request_id = :requestId
        AND sr.provider_id = :providerId
      """, nativeQuery = true)
    Optional<InvoicePageView> findInvoiceByIdForProvider(@Param("requestId") Integer requestId,
                                                         @Param("providerId") Integer providerId);

    // ======= View hoá đơn ràng buộc theo customer (CUSTOMER chỉ xem đơn của mình) =======
    @Query(value = """
      SELECT 
        sr.request_id AS invoiceId,
        CONVERT(VARCHAR(10), sr.request_date, 105) AS invoiceDate,
        sr.status AS status,
        CAST(sr.total_cost AS VARCHAR(50)) AS totalCost,
        sp.company_name AS providerCompany,
        CONCAT(a1.street_address, ', ', a1.city, ', ', a1.state) AS pickupAddress,
        CONCAT(a2.street_address, ', ', a2.city, ', ', a2.state) AS deliveryAddress,
        (
          SELECT fi.item_id, fi.item_type AS item_name, fi.size, fi.quantity, fi.is_fragile, fi.special_handling
          FROM furniture_items fi
          WHERE fi.request_id = sr.request_id
          FOR JSON PATH
        ) AS furnitureItemsJson
      FROM service_requests sr
      JOIN addresses a1 ON a1.address_id = sr.pickup_address_id
      JOIN addresses a2 ON a2.address_id = sr.delivery_address_id
      LEFT JOIN service_providers sp ON sp.provider_id = sr.provider_id
      WHERE sr.customer_id = :customerId
        AND sr.request_id  = :requestId
      """, nativeQuery = true)
    Optional<InvoicePageView> findInvoiceForCustomer(@Param("customerId") Integer customerId,
                                                     @Param("requestId") Integer requestId);

    // ======= Lấy đơn mới nhất của customer =======
    @Query(value = """
      SELECT TOP (1)
        sr.request_id AS invoiceId,
        CONVERT(VARCHAR(10), sr.request_date, 105) AS invoiceDate,
        sr.status AS status,
        CAST(sr.total_cost AS VARCHAR(50)) AS totalCost,
        sp.company_name AS providerCompany,
        CONCAT(a1.street_address, ', ', a1.city, ', ', a1.state) AS pickupAddress,
        CONCAT(a2.street_address, ', ', a2.city, ', ', a2.state) AS deliveryAddress,
        (
          SELECT fi.item_id, fi.item_type AS item_name, fi.size, fi.quantity, fi.is_fragile, fi.special_handling
          FROM furniture_items fi
          WHERE fi.request_id = sr.request_id
          FOR JSON PATH
        ) AS furnitureItemsJson
      FROM service_requests sr
      JOIN addresses a1 ON a1.address_id = sr.pickup_address_id
      JOIN addresses a2 ON a2.address_id = sr.delivery_address_id
      LEFT JOIN service_providers sp ON sp.provider_id = sr.provider_id
      WHERE sr.customer_id = :customerId
      ORDER BY sr.request_date DESC, sr.request_id DESC
      """, nativeQuery = true)
    Optional<InvoicePageView> findLatestInvoiceForCustomer(@Param("customerId") Integer customerId);

    // ======= Cập nhật trạng thái: pending -> completed (ADMIN duyệt mọi đơn) =======
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
      UPDATE service_requests
         SET status = 'completed'
       WHERE request_id = :id
         AND status = 'pending'
      """, nativeQuery = true)
    int approveIfPending(@Param("id") Integer id);

    // ======= Cập nhật trạng thái: pending -> completed (PROVIDER chỉ duyệt đơn của mình) =======
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
      UPDATE service_requests
         SET status = 'completed'
       WHERE request_id = :requestId
         AND status = 'pending'
         AND provider_id = :providerId
      """, nativeQuery = true)
    int approveIfPendingByProvider(@Param("requestId") Integer requestId,
                                   @Param("providerId") Integer providerId);

    long countByProviderId(Integer providerId);

    long countByProviderIdAndStatus(Integer providerId, String status);

    /* ---- PV-003: list orders for provider with search & status filter ---- */
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

    /* ---- PV-003: order detail for provider ---- */
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

    /* ---- PV-003: items of an order ---- */
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

    /* ---- PV-004: update status (and optional cancel reason) ---- */
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

}
