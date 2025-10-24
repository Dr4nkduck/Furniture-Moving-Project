package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.CheckoutRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;

@Service
public class CheckoutService {

    private final JdbcTemplate jdbcTemplate;

    public CheckoutService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer createPendingRequest(CheckoutRequest req) {
        // 1) Kiểm tra customer tồn tại và lấy user_id để ghi addresses
        Integer userId = getUserIdByCustomerId(req.getCustomerId());
        if (userId == null) {
            throw new IllegalArgumentException("customer_id không tồn tại: " + req.getCustomerId());
        }

        // 2) Tạo pickup address
        Integer pickupAddressId = insertAddress(
                userId, "home",
                req.getPickupAddress().streetAddress,
                req.getPickupAddress().city,
                req.getPickupAddress().state,
                req.getPickupAddress().zipCode
        );

        // 3) Tạo delivery address
        Integer deliveryAddressId = insertAddress(
                userId, "office",
                req.getDeliveryAddress().streetAddress,
                req.getDeliveryAddress().city,
                req.getDeliveryAddress().state,
                req.getDeliveryAddress().zipCode
        );

        // 4) Tạo service_requests (status = pending, provider_id = NULL)
        Integer requestId = insertServiceRequest(
                req.getCustomerId(),
                pickupAddressId,
                deliveryAddressId,
                req.getPreferredDate(),
                req.getTotalCost()
        );

        // 5) Ghi furniture_items
        if (req.getFurnitureItems() != null) {
            for (CheckoutRequest.FurnitureItemPayload it : req.getFurnitureItems()) {
                insertFurnitureItem(
                        requestId,
                        nullToDefault(it.itemType, "Unknown"),
                        it.size,
                        it.quantity == null ? 1 : it.quantity,
                        it.fragile != null && it.fragile,
                        it.specialHandling
                );
            }
        }

        return requestId;
    }

    private Integer getUserIdByCustomerId(Integer customerId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT user_id FROM customers WHERE customer_id = ?",
                    Integer.class, customerId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private Integer insertAddress(Integer userId, String addressType,
                                  String street, String city, String state, String zip) {
        final String sql = """
            INSERT INTO addresses (user_id, address_type, street_address, city, state, zip_code, is_default, created_at)
            VALUES (?, ?, ?, ?, ?, ?, 0, SYSUTCDATETIME())
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setString(2, addressType);
            ps.setString(3, street);
            ps.setString(4, city);
            ps.setString(5, state);
            ps.setString(6, zip);
            return ps;
        }, kh);
        return Objects.requireNonNull(kh.getKey()).intValue();
    }

    private Integer insertServiceRequest(Integer customerId,
                                         Integer pickupAddressId,
                                         Integer deliveryAddressId,
                                         java.time.LocalDate preferredDate,
                                         BigDecimal totalCost) {
        final String sql = """
            INSERT INTO service_requests
              (customer_id, provider_id, pickup_address_id, delivery_address_id, request_date, preferred_date, status, total_cost, created_at)
            VALUES
              (?, NULL, ?, ?, SYSUTCDATETIME(), ?, 'pending', ?, SYSUTCDATETIME())
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, customerId);
            ps.setInt(2, pickupAddressId);
            ps.setInt(3, deliveryAddressId);
            ps.setDate(4, Date.valueOf(preferredDate)); // DB column is DATE
            if (totalCost != null) {
                ps.setBigDecimal(5, totalCost);
            } else {
                ps.setBigDecimal(5, new BigDecimal("0.00"));
            }
            return ps;
        }, kh);
        return Objects.requireNonNull(kh.getKey()).intValue();
    }

    private void insertFurnitureItem(Integer requestId, String itemType, String size,
                                     Integer quantity, boolean fragile, String specialHandling) {
        final String sql = """
            INSERT INTO furniture_items
              (request_id, item_type, size, quantity, is_fragile, special_handling)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
                requestId, itemType, size, quantity, fragile ? 1 : 0, specialHandling);
    }

    private static String nullToDefault(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
