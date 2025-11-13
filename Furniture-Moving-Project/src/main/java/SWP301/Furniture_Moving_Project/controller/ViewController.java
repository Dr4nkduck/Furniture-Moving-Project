package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    private final UserRepository userRepository;

    public ViewController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** URL chính: http://localhost:8080/request */
    @GetMapping("/request")
    public String requestPage(Model model) {
        addLoginInfo(model); // ✅ Thêm dòng này
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
}
