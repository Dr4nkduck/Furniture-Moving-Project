package SWP301.Furniture_Moving_Project.config;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtils {
    private IpUtils() {}
    public static String clientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) {
            // lấy IP đầu tiên
            int comma = h.indexOf(',');
            return (comma >= 0) ? h.substring(0, comma).trim() : h.trim();
        }
        return req.getRemoteAddr();
    }
}
