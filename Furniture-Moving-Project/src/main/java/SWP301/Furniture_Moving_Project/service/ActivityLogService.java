package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.model.ActivityLog;
import SWP301.Furniture_Moving_Project.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ActivityLogService {
    private final ActivityLogRepository repo;

    public ActivityLogService(ActivityLogRepository repo) {
        this.repo = repo;
    }

    public void log(Integer userId, String type, String description, String ip) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setEventType(type);
        log.setEventDescription(description);
        log.setIpAddress(ip);
        log.setTimestamp(OffsetDateTime.now());
        repo.save(log);
    }
}
