package SWP301.Furniture_Moving_Project.model;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CustomRevisionListener implements RevisionListener {
    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;

        String modifiedBy = "system"; // default

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                modifiedBy = auth.getName(); // lấy username trong Spring Security
            }
        } catch (Exception e) {
            // fallback
        }

        rev.setModifiedBy(modifiedBy);
    }
}
