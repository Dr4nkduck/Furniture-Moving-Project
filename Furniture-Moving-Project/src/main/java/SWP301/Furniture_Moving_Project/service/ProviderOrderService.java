package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.ProviderOrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderSummaryDTO;

import java.util.List;

public interface ProviderOrderService {
    List<ProviderOrderSummaryDTO> searchOrders(Integer providerId, String q, List<String> statuses);
    ProviderOrderDetailDTO getOrderDetail(Integer providerId, Integer requestId);
    String updateOrderStatus(Integer providerId, Integer requestId, String newStatus);
}
