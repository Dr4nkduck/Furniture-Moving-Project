package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.OrderSummaryDTO;

import java.util.List;

public interface OrderQueryService {
    List<OrderSummaryDTO> findRecentForUser(Integer userId, int limit);
}
