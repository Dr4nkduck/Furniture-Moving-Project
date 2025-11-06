package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.ProviderOrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderItemDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderOrderSummaryDTO;
import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.FurnitureItemRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.ProviderOrderService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ProviderOrderServiceImpl implements ProviderOrderService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final FurnitureItemRepository furnitureItemRepository;

    public ProviderOrderServiceImpl(ServiceRequestRepository serviceRequestRepository,
                                    FurnitureItemRepository furnitureItemRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.furnitureItemRepository = furnitureItemRepository;
    }

    @Override
    public List<ProviderOrderSummaryDTO> searchOrders(Integer providerId, String q, List<String> statuses) {
        var list = serviceRequestRepository.searchForProvider(
                providerId,
                (statuses == null || statuses.isEmpty()) ? null : statuses,
                (StringUtils.hasText(q) ? q.trim() : null)
        );

        List<ProviderOrderSummaryDTO> dtos = new ArrayList<>();
        for (ServiceRequest sr : list) {
            String customerName = sr.getCustomer().getUser().getFirstName() + " " + sr.getCustomer().getUser().getLastName();
            String pickup = sr.getPickupAddress().getStreetAddress() + ", " + sr.getPickupAddress().getCity();
            String delivery = sr.getDeliveryAddress().getStreetAddress() + ", " + sr.getDeliveryAddress().getCity();

            dtos.add(new ProviderOrderSummaryDTO(
                    sr.getRequestId(),
                    sr.getStatus(),
                    sr.getPreferredDate(),
                    customerName,
                    pickup,
                    delivery,
                    sr.getTotalCost()
            ));
        }
        return dtos;
    }

    @Override
    public ProviderOrderDetailDTO getOrderDetail(Integer providerId, Integer requestId) {
        var sr = serviceRequestRepository.findDetailForProvider(providerId, requestId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found or not owned by provider"));

        ProviderOrderDetailDTO dto = new ProviderOrderDetailDTO();
        dto.setRequestId(sr.getRequestId());
        dto.setStatus(sr.getStatus());
        dto.setPreferredDate(sr.getPreferredDate());
        dto.setCustomerName(sr.getCustomer().getUser().getFirstName() + " " + sr.getCustomer().getUser().getLastName());
        dto.setCustomerPhone(sr.getCustomer().getUser().getPhone());
        dto.setPickupAddress(sr.getPickupAddress().getStreetAddress() + ", " + sr.getPickupAddress().getCity());
        dto.setDeliveryAddress(sr.getDeliveryAddress().getStreetAddress() + ", " + sr.getDeliveryAddress().getCity());
        dto.setTotalCost(sr.getTotalCost());

        List<FurnitureItem> items = furnitureItemRepository.findByRequest_RequestId(sr.getRequestId());
        List<ProviderOrderItemDTO> itemDTOs = new ArrayList<>();
        for (FurnitureItem fi : items) {
            itemDTOs.add(new ProviderOrderItemDTO(
                    fi.getItemType(),
                    fi.getSize(),
                    fi.getQuantity(),
                    fi.isFragile()
            ));
        }
        dto.setItems(itemDTOs);
        return dto;
    }

    @Override
    public String updateOrderStatus(Integer providerId, Integer requestId, String newStatus) {
        newStatus = (newStatus == null) ? "" : newStatus.trim().toLowerCase();
        // Cho phép: pending -> in_progress -> completed; mọi trạng thái -> cancelled (khi KH hủy)
        var sr = serviceRequestRepository.findDetailForProvider(providerId, requestId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found or not owned by provider"));
        String cur = sr.getStatus().toLowerCase();

        boolean ok = switch (newStatus) {
            case "in_progress" -> cur.equals("pending") || cur.equals("assigned");
            case "completed"   -> cur.equals("in_progress");
            case "cancelled"   -> !cur.equals("completed");
            default            -> false;
        };

        if (!ok) {
            throw new IllegalStateException("Invalid status transition: " + cur + " → " + newStatus);
        }
        sr.setStatus(newStatus);
        serviceRequestRepository.save(sr);
        return newStatus;
    }
}
