package SWP301.Furniture_Moving_Project.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final JdbcTemplate jdbcTemplate;

    public HomeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

        // View name khớp với templates/homepage/homepage.html
        return "homepage/homepage";
    }
}
