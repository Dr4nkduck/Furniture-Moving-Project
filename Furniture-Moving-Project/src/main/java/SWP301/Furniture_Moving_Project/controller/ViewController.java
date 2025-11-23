package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;

    public ViewController(UserRepository userRepository, JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.jdbc = jdbc;
    }

    /** URL chính: http://localhost:8080/request */
    @GetMapping("/request")
    public String requestPage(Model model) {
        addLoginInfo(model);
        // Get currentCustomerId for the form
        try {
            Integer customerId = getCurrentCustomerId();
            if (customerId != null) {
                model.addAttribute("currentCustomerId", customerId);
            }
        } catch (Exception e) {
            // User not logged in or not a customer - leave currentCustomerId null
        }
        return "request/request"; // templates/request/request.html
    }

    /** Giữ url cũ: chuyển hướng về /request */
    @GetMapping({"/requestservice", "/request-service"})
    public String legacyRequestUrls() {
        return "redirect:/request";
    }

    // KHÔNG map "/" ở đây để tránh đụng HomeController


    /** ============================================================
     *  HÀM DÙNG CHUNG CHO NAVBAR
     *  ============================================================ */
    private void addLoginInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            model.addAttribute("isLoggedIn", true);

            Object principal = auth.getPrincipal();
            String username = null;
            try {
                username = (String) principal.getClass().getMethod("getUsername").invoke(principal);
            } catch (Exception ignored) {
            }

            if (username != null) {
                userRepository.findByUsername(username).ifPresent(u -> {
                    model.addAttribute("currentUser", u);
                });
            }
        } else {
            model.addAttribute("isLoggedIn", false);
        }
    }

    /**
     * Lấy customer_id tương ứng user đang login
     * users.username -> customers.customer_id
     */
    private Integer getCurrentCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String username = auth.getName();

        try {
            return jdbc.queryForObject(
                    """
                    SELECT c.customer_id
                    FROM customers c
                    JOIN users u ON u.user_id = c.user_id
                    WHERE u.username = ?
                    """,
                    Integer.class,
                    username
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }
}
