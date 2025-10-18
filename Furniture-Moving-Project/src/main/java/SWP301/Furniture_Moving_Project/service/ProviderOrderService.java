package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.OrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.OrderSummaryDTO;
import java.util.List;

public interface ProviderOrderService {
    List<OrderSummaryDTO> listOrders(Long providerId, List<String> statuses);
    OrderDetailDTO getOrderDetail(Long providerId, Long orderId, boolean logPiiRead);
    void acceptRfp(Long providerId, Long orderId, String note);
    void declineRfp(Long providerId, Long orderId, String reason);
    void updateStatus(Long providerId, Long orderId, String nextStatus, String etaText, String note);
}
