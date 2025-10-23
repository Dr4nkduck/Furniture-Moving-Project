package SWP301.Furniture_Moving_Project.dto;

public record CustomerFunnelDTO(
        String fromDate, String toDate,
        long searches, long quotes, long bookings
) {}
