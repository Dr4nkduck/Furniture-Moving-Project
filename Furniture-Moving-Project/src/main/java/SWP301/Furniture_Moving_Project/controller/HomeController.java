package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public HomeController(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    // Redirect root -> /homepage
    @GetMapping("/")
    public String root() {
        return "redirect:/homepage";
    }

    // Render templates/homepage/homepage.html
    @GetMapping("/homepage")
    public String homepage(Model model) {

        // ===== 1. Check DB như cũ =====
        boolean dbOk = false;
        String msg = "Không thể kết nối SQL Server";
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbOk = (one != null && one == 1);
            if (dbOk) msg = "Kết nối SQL Server: OK";
        } catch (Exception e) {
            msg = "Kết nối SQL Server lỗi: " + e.getMessage();
        }
        model.addAttribute("dbOk", dbOk);
        model.addAttribute("dbMsg", msg);

        // ===== 2. Lấy thông tin đăng nhập hiện tại =====
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {

            // Lấy danh sách role từ Authentication
            Set<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // 👉 Nếu là SUPER_ADMIN hoặc PROVIDER thì KHÔNG cho vào homepage
            if (roles.contains("ROLE_SUPER_ADMIN")) {
                return "redirect:/superadmin/users";      // thay đúng URL dashboard superadmin của bạn
            }
            if (roles.contains("ROLE_PROVIDER")) {
                return "redirect:/provider/services";     // thay đúng URL dashboard provider
            }

            // Nếu là CUSTOMER (hoặc role khác) thì load info user như cũ
            Optional<User> userOpt = userRepository.findByUsername(auth.getName());
            userOpt.ifPresent(user -> model.addAttribute("currentUser", user));
            model.addAttribute("isLoggedIn", true);

        } else {
            // Chưa đăng nhập → vẫn vào homepage bình thường
            model.addAttribute("isLoggedIn", false);
        }

        // View name khớp với templates/homepage/homepage.html
        return "homepage/homepage";
    }

    // Convenience route used in carousel/linking
    @GetMapping("/homepage/providers")
    public String homepageProviders() {
        return "redirect:/providers";
    }
}
