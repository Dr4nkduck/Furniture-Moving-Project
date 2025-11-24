package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderReviewDTO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/provider/reviews")
public class ProviderReviewApiController {

    private final JdbcTemplate jdbc;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ProviderReviewApiController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lấy provider_id từ user đang đăng nhập */
    private Integer getCurrentProviderId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();

        try {
            return jdbc.queryForObject("""
                SELECT p.provider_id
                FROM providers p
                JOIN users u ON u.user_id = p.user_id
                WHERE u.username = ?
            """, Integer.class, username);
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Provider not found");
        }
    }

    /** Danh sách review của provider đang đăng nhập (dùng cho trang /provider/reviews) */
    @GetMapping
    public List<ProviderReviewDTO> listMyReviews() {
        Integer providerId = getCurrentProviderId();

        String sql = """
            SELECT r.review_id,
                   r.request_id,
                   r.rating,
                   r.comment,
                   r.created_at
            FROM reviews r
            WHERE r.provider_id = ?
            ORDER BY r.created_at DESC
            """;

        return jdbc.query(sql, (rs, rowNum) -> {
            ProviderReviewDTO dto = new ProviderReviewDTO();
            dto.setReviewId(rs.getInt("review_id"));
            dto.setRequestId(rs.getInt("request_id"));
            dto.setRating(rs.getInt("rating"));
            dto.setComment(rs.getString("comment"));

            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) {
                LocalDateTime ldt = ts.toLocalDateTime();
                dto.setCreatedAt(dtf.format(ldt));
            }
            return dto;
        }, providerId);
    }

    /** ✅ Lấy review của MỘT request cụ thể, đảm bảo thuộc về provider đang đăng nhập */
    @GetMapping("/by-request/{requestId}")
    public ProviderReviewDTO getReviewForRequest(@PathVariable("requestId") Integer requestId) {
        Integer providerId = getCurrentProviderId();

        String sql = """
            SELECT r.review_id,
                   r.request_id,
                   r.rating,
                   r.comment,
                   r.created_at
            FROM reviews r
            WHERE r.request_id = ? AND r.provider_id = ?
            """;

        try {
            return jdbc.queryForObject(sql, (rs, rowNum) -> {
                ProviderReviewDTO dto = new ProviderReviewDTO();
                dto.setReviewId(rs.getInt("review_id"));
                dto.setRequestId(rs.getInt("request_id"));
                dto.setRating(rs.getInt("rating"));
                dto.setComment(rs.getString("comment"));

                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    LocalDateTime ldt = ts.toLocalDateTime();
                    dto.setCreatedAt(dtf.format(ldt));
                }
                return dto;
            }, requestId, providerId);
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá cho đơn này");
        }
    }
}
