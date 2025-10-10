package SWP301.Furniture_Moving_Project.controller.dto;

import SWP301.Furniture_Moving_Project.model.AccountStatus;
import java.time.OffsetDateTime;

public record UserAccountResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String primaryRole,
        AccountStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
