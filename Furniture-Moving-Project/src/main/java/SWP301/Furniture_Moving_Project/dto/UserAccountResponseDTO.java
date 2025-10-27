package SWP301.Furniture_Moving_Project.dto;

import SWP301.Furniture_Moving_Project.model.AccountStatus;
import java.time.OffsetDateTime;

public record UserAccountResponseDTO(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String primaryRole,       // can be null if you don't resolve it
        AccountStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
