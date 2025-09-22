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
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            res.sendRedirect(req.getContextPath() + "/admin/dashboard");
        } else {
            res.sendRedirect(req.getContextPath() + "/user");
        }
    }
}
