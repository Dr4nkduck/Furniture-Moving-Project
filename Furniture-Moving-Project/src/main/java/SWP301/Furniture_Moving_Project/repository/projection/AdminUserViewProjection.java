package SWP301.Furniture_Moving_Project.repository.projection;

import java.time.OffsetDateTime;

public interface AdminUserViewProjection {
    Long getId();

    String getUsername();

    String getEmail();

    String getPhone();

    String getFirstName();

    String getLastName();

    String getPrimaryRole();

    String getStatus();        // raw DB string; we map -> enum in service

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();
}
