package SWP301.Furniture_Moving_Project.config;

import SWP301.Furniture_Moving_Project.repository.UserRepository;
import SWP301.Furniture_Moving_Project.service.ActivityLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepo;
    private final ActivityLogService activityLogService;

    public RoleBasedAuthSuccessHandler(UserRepository userRepo,
                                       ActivityLogService activityLogService) {
        this.userRepo = userRepo;
        this.activityLogService = activityLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication auth) throws IOException, ServletException {

        // --- GHI LOG LOGIN ---
        String username = auth.getName();
        Integer userId = userRepo.findByUsername(username).map(u -> u.getUserId()).orElse(null);
        String ip = IpUtils.clientIp(req);
        activityLogService.log(userId, "LOGIN", "Đăng nhập", ip);

        String ctx = req.getContextPath();

        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            // ✅ sau login vào Dashboard của Super Admin
            res.sendRedirect(ctx + "/super/dashboard");
            return;
        }
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            res.sendRedirect(ctx + "/admin/dashboard");
            return;
        }
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROVIDER"))) {
            res.sendRedirect(ctx + "/provider");
            return;
        }
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            res.sendRedirect(ctx + "/homepage");
            return;
        }
        res.sendRedirect(ctx + "/user");
    }
}
