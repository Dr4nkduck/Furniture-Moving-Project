package SWP301.Furniture_Moving_Project.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedAuthSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication auth) throws IOException, ServletException {

        String ctx = req.getContextPath();

        // ƯU TIÊN CAO NHẤT
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            res.sendRedirect(ctx + "/super");
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

        // Mặc định: CUSTOMER hoặc các role khác
        res.sendRedirect(ctx + "/user");
    }
}
