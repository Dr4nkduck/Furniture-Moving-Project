package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.RateLimitConfigDto;   // ✅ dto (chữ thường)
import SWP301.Furniture_Moving_Project.dto.RateLimitStatusDto;  // ✅ dto (chữ thường)
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.model.UserRateLimit;
import SWP301.Furniture_Moving_Project.model.UserRateLimitLog;
import SWP301.Furniture_Moving_Project.repository.UserRateLimitLogRepository;
import SWP301.Furniture_Moving_Project.repository.UserRateLimitRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RateLimitService {

    private final UserRateLimitRepository rateLimitRepository;
    private final UserRateLimitLogRepository logRepository;
    private final UserRepository userRepository;

    public RateLimitService(UserRateLimitRepository rateLimitRepository,
                            UserRateLimitLogRepository logRepository,
                            UserRepository userRepository) {
        this.rateLimitRepository = rateLimitRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
    }

    /** Check if user has exceeded rate limit */
    public boolean isRateLimitExceeded(Integer userId) {
        Optional<UserRateLimit> limitOpt = rateLimitRepository.findByUserIdAndStatus(userId, "enabled");
        if (limitOpt.isEmpty()) {
            return false; // No rate limit configured
        }

        UserRateLimit limit = limitOpt.get();
        OffsetDateTime since = OffsetDateTime.now().minusSeconds(limit.getTimeWindowSeconds());
        long requestCount = logRepository.countByUserIdAndRequestTimestampAfter(userId, since);
        return requestCount >= limit.getMaxRequests();
    }

    /** Log user request */
    @Transactional
    public void logRequest(Integer userId, String requestPath, String requestMethod, String ipAddress, boolean wasBlocked) {
        UserRateLimitLog log = new UserRateLimitLog();
        log.setUserId(userId);
        log.setRequestPath(requestPath);
        log.setRequestMethod(requestMethod);
        log.setIpAddress(ipAddress);
        log.setWasBlocked(wasBlocked);
        logRepository.save(log);
    }

    /** Create or update rate limit for user */
    @Transactional
    public UserRateLimit setRateLimit(RateLimitConfigDto configDto, Integer adminId) {
        Optional<UserRateLimit> existingOpt = rateLimitRepository.findByUserId(configDto.getUserId());

        UserRateLimit rateLimit;
        if (existingOpt.isPresent()) {
            rateLimit = existingOpt.get();
        } else {
            rateLimit = new UserRateLimit();
            rateLimit.setUserId(configDto.getUserId());
            rateLimit.setCreatedBy(adminId);
        }

        rateLimit.setMaxRequests(configDto.getMaxRequests());
        rateLimit.setTimeWindowSeconds(configDto.getTimeWindowSeconds());
        rateLimit.setStatus(configDto.getStatus() != null ? configDto.getStatus() : "enabled");
        rateLimit.setNotes(configDto.getNotes());

        return rateLimitRepository.save(rateLimit);
    }

    /** Get rate limit status for user */
    public RateLimitStatusDto getRateLimitStatus(Integer userId) {
        Optional<UserRateLimit> limitOpt = rateLimitRepository.findByUserId(userId);
        Optional<User> userOpt = userRepository.findById(userId);

        RateLimitStatusDto dto = new RateLimitStatusDto();
        dto.setUserId(userId);

        userOpt.ifPresent(u -> dto.setUsername(u.getUsername()));

        if (limitOpt.isPresent()) {
            UserRateLimit limit = limitOpt.get();
            dto.setMaxRequests(limit.getMaxRequests());
            dto.setTimeWindowSeconds(limit.getTimeWindowSeconds());
            dto.setStatus(limit.getStatus());
            dto.setNotes(limit.getNotes());

            if ("enabled".equals(limit.getStatus())) {
                OffsetDateTime since = OffsetDateTime.now().minusSeconds(limit.getTimeWindowSeconds());
                long currentCount = logRepository.countByUserIdAndRequestTimestampAfter(userId, since);
                dto.setCurrentRequestCount(currentCount);
                dto.setRemainingRequests(Math.max(0, limit.getMaxRequests() - currentCount));
            }
        } else {
            dto.setStatus("not_configured");
            dto.setMaxRequests(0);
            dto.setTimeWindowSeconds(0);
            dto.setCurrentRequestCount(0L);
            dto.setRemainingRequests(0L);
        }

        return dto;
    }

    /** Get all rate limits */
    public List<UserRateLimit> getAllRateLimits() {
        return rateLimitRepository.findAll();
    }

    /** Delete rate limit for user */
    @Transactional
    public void deleteRateLimit(Integer userId) {
        rateLimitRepository.findByUserId(userId).ifPresent(rateLimitRepository::delete);
    }

    /** Clean up old logs (scheduled task - runs daily) */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM every day
    @Transactional
    public void cleanupOldLogs() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(30); // Keep logs for 30 days
        logRepository.deleteByRequestTimestampBefore(threshold);
    }

    /** Get recent logs for user */
    public List<UserRateLimitLog> getRecentLogs(Integer userId, int hours) {
        OffsetDateTime since = OffsetDateTime.now().minusHours(hours);
        return logRepository.findByUserIdAndRequestTimestampAfterOrderByRequestTimestampDesc(userId, since);
    }
}
