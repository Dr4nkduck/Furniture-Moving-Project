package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.ProviderOrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderItemDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderSummaryDTO;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderDetailProjection;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderItemProjection;
import SWP301.Furniture_Moving_Project.repository.projection.ProviderOrderSummaryProjection;
import SWP301.Furniture_Moving_Project.service.ProviderOrderService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProviderOrderServiceImpl implements ProviderOrderService {

    private final ServiceRequestRepository srRepo;

    public ProviderOrderServiceImpl(ServiceRequestRepository srRepo) {
        this.srRepo = srRepo;
    }

    @Override
    public List<ProviderOrderSummaryDTO> listOrders(Integer providerId, String status, String q) {
        String s = StringUtils.hasText(status) ? status : null;
        String query = StringUtils.hasText(q) ? q.trim() : null;

        List<ProviderOrderSummaryProjection> rows = srRepo.findProviderOrders(providerId, s, query);
        return rows.stream().map(r -> new ProviderOrderSummaryDTO(
                r.getRequestId(),
                r.getStatus(),
                r.getRequestDate(),
                r.getPreferredDate(),
                (r.getCustomerFirstName() == null && r.getCustomerLastName() == null)
                        ? "N/A" : (r.getCustomerFirstName() + " " + r.getCustomerLastName()).trim(),
                join(r.getPickupStreet(), r.getPickupCity()),
                join(r.getDeliveryStreet(), r.getDeliveryCity()),
                r.getTotalCost()
        )).collect(Collectors.toList());
    }

    @Override
    public ProviderOrderDetailDTO getOrderDetail(Integer providerId, Integer requestId) {
        ProviderOrderDetailProjection p = srRepo.findOrderDetail(providerId, requestId);
        if (p == null) throw new IllegalArgumentException("Order not found or not owned by provider");

        ProviderOrderDetailDTO dto = new ProviderOrderDetailDTO();
        dto.setRequestId(p.getRequestId());
        dto.setStatus(p.getStatus());
        dto.setRequestDate(p.getRequestDate());
        dto.setPreferredDate(p.getPreferredDate());
        dto.setTotalCostEstimate(p.getTotalCost());

        dto.setCustomerName((p.getCustomerFirstName() + " " + p.getCustomerLastName()).trim());
        dto.setCustomerPhone(p.getCustomerPhone());
        dto.setCustomerEmail(p.getCustomerEmail());

        dto.setPickupFull(joinFull(p.getPickupStreet(), p.getPickupCity(), p.getPickupState(), p.getPickupZip()));
        dto.setDeliveryFull(joinFull(p.getDeliveryStreet(), p.getDeliveryCity(), p.getDeliveryState(), p.getDeliveryZip()));

        List<ProviderOrderItemProjection> items = srRepo.findOrderItems(requestId);
        dto.setItems(items.stream()
                .map(i -> new ProviderOrderItemDTO(i.getItemId(), i.getItemType(), i.getSize(),
                        i.getQuantity() == null ? 0 : i.getQuantity(), Boolean.TRUE.equals(i.getIsFragile())))
                .collect(Collectors.toList()));
        return dto;
    }

    @Override
    public void updateOrderStatus(Integer providerId, Integer requestId, String newStatus, String cancelReason) {
        if (!StringUtils.hasText(newStatus)) {
            throw new IllegalArgumentException("Missing status");
        }
        String ns = newStatus.toLowerCase();

        // transition rules (map theo yêu cầu PV-004)
        // pending -> accepted/declined/cancelled
        // accepted -> in_progress/cancelled
        // in_progress -> completed/cancelled
        // completed/declined/cancelled -> only allow cancelled (idempotent) else reject
        // (ở đây kiểm tra mức tối thiểu phía service; có thể mở rộng bằng cách đọc current status trước)
        switch (ns) {
            case "pending", "accepted", "declined", "in_progress", "completed", "cancelled" -> {}
            default -> throw new IllegalArgumentException("Unsupported status: " + ns);
        }

        int updated = srRepo.providerUpdateStatus(providerId, requestId, ns,
                StringUtils.hasText(cancelReason) && "cancelled".equals(ns) ? cancelReason : null);
        if (updated == 0) throw new IllegalArgumentException("Order not found or not owned by provider");
    }

    private static String join(String a, String b) {
        if (!StringUtils.hasText(a)) return StringUtils.hasText(b) ? b : "";
        return StringUtils.hasText(b) ? a + ", " + b : a;
    }

    private static String joinFull(String street, String city, String state, String zip) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(street)) sb.append(street);
        if (StringUtils.hasText(city)) sb.append(sb.length()>0?", ":"").append(city);
        if (StringUtils.hasText(state)) sb.append(sb.length()>0?", ":"").append(state);
        if (StringUtils.hasText(zip)) sb.append(" ").append(zip);
        return sb.toString();
    }
}
