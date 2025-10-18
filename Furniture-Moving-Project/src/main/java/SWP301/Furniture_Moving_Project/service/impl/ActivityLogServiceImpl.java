package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.model.ActivityLog;
import SWP301.Furniture_Moving_Project.repository.ActivityLogRepository;
import SWP301.Furniture_Moving_Project.service.ActivityLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository repo;

    public ActivityLogServiceImpl(ActivityLogRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void log(Long actorId, String type, String details) {
        ActivityLog log = new ActivityLog();

        // If your ActivityLog.userId is Integer, convert; if it's Long, change to setUserId(actorId).
        if (actorId != null) {
            try {
                // Common in your codebase: Integer userId
                log.setUserId(actorId.intValue());
            } catch (NoSuchMethodError | RuntimeException ignore) {
                // If your entity uses Long:
                // log.setUserId(actorId);
            }
        }

        // ---- Adjust ONE of these if your field names differ ----
        log.setType(type);           // or: log.setActionType(type); / log.setAction(type);
        log.setDetails(details);     // keep
        log.setCreatedAt(LocalDateTime.now());

        // Optional IP capture
        String ip = null;
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null && attrs.getRequest() != null) {
            ip = attrs.getRequest().getRemoteAddr();
        }
        try { log.setIpAddress(ip); } catch (NoSuchMethodError | RuntimeException ignore) {
            // field not present in your entity — safe to ignore
        }

        repo.save(log);
    }
}
