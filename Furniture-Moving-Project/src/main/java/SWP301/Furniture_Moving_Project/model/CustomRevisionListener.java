// src/main/java/SWP301/Furniture_Moving_Project/model/CustomRevisionListener.java
package SWP301.Furniture_Moving_Project.model;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Gán "modifiedBy" cho mỗi revision. Ở đây lấy username từ SecurityContext;
 * nếu không có auth thì gán "system".
 */
public class CustomRevisionListener implements RevisionListener {
    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;

        String who = "system";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                who = auth.getName();
            }
        } catch (Exception ignored) { /* giữ an toàn nếu chạy ngoài context security */ }

        rev.setModifiedBy(who);
    }
}
