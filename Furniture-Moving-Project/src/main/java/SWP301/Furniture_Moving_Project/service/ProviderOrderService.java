package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.ProviderOrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderSummaryDTO;

import java.util.List;

public interface ProviderOrderService {

    List<ProviderOrderSummaryDTO> listOrders(Integer providerId, String status, String q);

    ProviderOrderDetailDTO getOrderDetail(Integer providerId, Integer requestId);

    void updateOrderStatus(Integer providerId, Integer requestId, String newStatus, String cancelReason);

    /**
     * Provider xác nhận đơn đã được thanh toán (sau khi tự kiểm tra sao kê ngân hàng).
     * Chỉ nên cho phép khi đơn thuộc provider này và đang ở trạng thái "ready_to_pay".
     */
    void confirmPayment(Integer providerId, Integer requestId);
}
