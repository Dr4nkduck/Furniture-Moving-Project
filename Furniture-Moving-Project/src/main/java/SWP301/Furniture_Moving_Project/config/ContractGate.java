package SWP301.Furniture_Moving_Project.config;

import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import SWP301.Furniture_Moving_Project.service.ContractService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class ContractGate implements HandlerInterceptor {

    private final ContractService contracts;
    private final UserRepository users;

    public ContractGate(ContractService contracts, UserRepository users) {
        this.contracts = contracts;
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String uri = req.getRequestURI();

        // Chỉ chặn /request (không nhánh phụ) và toàn bộ /payment/**
        if (!"/request".equals(uri) && !uri.startsWith("/payment")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            String next = URLEncoder.encode(uri, StandardCharsets.UTF_8);
            res.sendRedirect("/login?next=" + next);
            return false;
        }

        Integer userId = resolveCurrentUserId(auth);
        if (userId == null) {
            // Không xác định được user → yêu cầu đăng nhập lại
            res.sendRedirect("/login");
            return false;
        }

        if (!contracts.hasAccepted(userId)) {
            res.sendRedirect("/contract");
            return false;
        }
        return true;
    }

    // ===== helpers =====
    private Integer resolveCurrentUserId(Authentication auth) {
        Object p = auth.getPrincipal();

        // Case 1: principal là entity User
        if (p instanceof User) {
            return ((User) p).getUserId();
        }

        // Case 2: Spring Security UserDetails mặc định
        if (p instanceof org.springframework.security.core.userdetails.User springUser) {
            String username = springUser.getUsername();
            // Ưu tiên tra theo username (ignore-case)
            Optional<User> byUsername = users.findByUsernameIgnoreCase(username);
            if (byUsername.isPresent()) return byUsername.get().getUserId();

            // fallback: nếu hệ thống dùng email làm username
            Optional<User> byEmail = users.findByEmailIgnoreCase(username);
            if (byEmail.isPresent()) return byEmail.get().getUserId();

            // fallback nữa: tra đúng-case
            return users.findByUsername(username)
                        .map(User::getUserId)
                        .orElse(null);
        }

        // Case 3: chưa đăng nhập (anonymousUser) hoặc loại khác
        if (p instanceof String s && "anonymousUser".equals(s)) {
            return null;
        }

        // Trường hợp custom principal khác: có thể bổ sung ở đây nếu cần
        return null;
    }
}
