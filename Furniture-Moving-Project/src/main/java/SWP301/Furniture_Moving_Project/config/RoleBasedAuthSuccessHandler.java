package SWP301.Furniture_Moving_Project.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
            throws IOException, ServletException {

        final String ctx = req.getContextPath() == null ? "" : req.getContextPath();
        var authorities = auth.getAuthorities();

        boolean isSuper = authorities.stream().anyMatch(a -> "ROLE_SUPERADMIN".equals(a.getAuthority()));
        boolean isAdmin = authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isProvider = authorities.stream().anyMatch(a -> "ROLE_PROVIDER".equals(a.getAuthority()));

        if (isSuper) {
            res.sendRedirect(ctx + "/super/dashboard");
            return;
        }
        if (isAdmin) {
            res.sendRedirect(ctx + "/admin/dashboard");
            return;
        }
        if (isProvider) {
            //changed from "/provider" to the new dashboard route
            res.sendRedirect(ctx + "/provider/dashboard");
            return;
        }
        // default (e.g., customer)
        res.sendRedirect(ctx + "/user");
    }
}
