package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

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

        // Thêm thông tin user nếu đã đăng nhập
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Optional<User> userOpt = userRepository.findByUsername(auth.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("currentUser", user);
                model.addAttribute("isLoggedIn", true);
            }
        } else {
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
