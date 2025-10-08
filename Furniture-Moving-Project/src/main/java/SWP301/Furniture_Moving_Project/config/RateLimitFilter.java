package SWP301.Furniture_Moving_Project.config;

import SWP301.Furniture_Moving_Project.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting for public endpoints
        String requestPath = request.getRequestURI();
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for admin users
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
            && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get user ID
        Integer userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check rate limit
        boolean isExceeded = rateLimitService.isRateLimitExceeded(userId);

        // Log the request
        String ipAddress = getClientIpAddress(request);
        rateLimitService.logRequest(userId, requestPath, request.getMethod(), ipAddress, isExceeded);

        if (isExceeded) {
            response.setStatus(429); // 429 Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.equals("/") || path.startsWith("/homepage") || path.startsWith("/login") 
            || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/");
    }

    private Integer getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            // You may need to adjust this based on your CustomUserDetails implementation
            String username = ((UserDetails) principal).getUsername();
            // Parse userId from username or use a custom method
            // For now, returning null as we need to see your CustomUserDetails implementation
            return null; // TODO: Implement based on your UserDetails structure
        }

        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
