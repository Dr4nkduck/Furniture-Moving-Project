package SWP301.Furniture_Moving_Project.dto;

import java.time.LocalDateTime;

public record OrderSummaryDTO(Integer id, String code, String status, LocalDateTime createdAt) {}
