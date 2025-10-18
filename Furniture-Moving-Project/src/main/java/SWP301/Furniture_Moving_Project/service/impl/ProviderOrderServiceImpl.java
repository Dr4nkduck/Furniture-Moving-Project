package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.OrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.OrderSummaryDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.model.OrderStatus;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.ActivityLogService;
import SWP301.Furniture_Moving_Project.service.ProviderOrderService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProviderOrderServiceImpl implements ProviderOrderService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ProviderRepository providerRepository;
    private final ActivityLogService activityLogService;

    public ProviderOrderServiceImpl(ServiceRequestRepository serviceRequestRepository,
                                    ProviderRepository providerRepository,
                                    ActivityLogService activityLogService) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.providerRepository = providerRepository;
        this.activityLogService = activityLogService;
    }

    @Override
    public List<OrderSummaryDTO> listOrders(Long providerId, List<String> statuses) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found"));

        List<OrderStatus> statusEnums = statuses.stream()
                .map(s -> OrderStatus.valueOf(s.trim()))
                .collect(Collectors.toList());

        return serviceRequestRepository.findByProviderIdAndStatusIn(provider.getProviderId(), statusEnums)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDetailDTO getOrderDetail(Long providerId, Long orderId, boolean shouldLogPiiRead) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (serviceRequest.getProvider() == null
                || !Objects.equals(serviceRequest.getProvider().getProviderId(), providerId)) {
            throw new EntityNotFoundException("Not your order");
        }

        if (shouldLogPiiRead) {
            activityLogService.log(
                    providerId,
                    "ORDER_PII_READ",
                    "providerId=" + providerId + ", orderId=" + orderId
            );
        }

        return toDetail(serviceRequest, true);
    }

    @Override
    @Transactional
    public void acceptRfp(Long providerId, Long orderId, String note) {
        ServiceRequest serviceRequest = requireOwnedByProvider(providerId, orderId);

        if (serviceRequest.getStatus() != OrderStatus.PENDING_OFFER) {
            throw new IllegalStateException("Not in RFP state");
        }

        serviceRequest.setStatus(OrderStatus.ASSIGNED);
        serviceRequest.setProvider(providerRepository.getReferenceById(providerId));
        if (note != null && !note.isBlank()) {
            serviceRequest.setNotes(note);
        }
        serviceRequestRepository.save(serviceRequest);

        activityLogService.log(
                providerId,
                "ORDER_ACCEPT",
                "providerId=" + providerId + ", orderId=" + orderId
                        + (note != null && !note.isBlank() ? ", note=" + note : "")
        );
    }

    @Override
    @Transactional
    public void declineRfp(Long providerId, Long orderId, String reason) {
        ServiceRequest serviceRequest = requireOwnedByProvider(providerId, orderId);

        if (serviceRequest.getStatus() != OrderStatus.PENDING_OFFER) {
            throw new IllegalStateException("Not in RFP state");
        }

        activityLogService.log(
                providerId,
                "ORDER_DECLINE",
                "providerId=" + providerId + ", orderId=" + orderId
                        + (reason != null && !reason.isBlank() ? ", reason=" + reason : "")
        );
    }

    @Override
    @Transactional
    public void updateStatus(Long providerId, Long orderId, String targetStatus, String etaText, String note) {
        ServiceRequest serviceRequest = requireOwnedByProvider(providerId, orderId);
        OrderStatus to = OrderStatus.valueOf(targetStatus);

        boolean allowed =
                (serviceRequest.getStatus() == OrderStatus.ASSIGNED
                        && (to == OrderStatus.IN_PROGRESS || to == OrderStatus.CANCELLED))
                        || (serviceRequest.getStatus() == OrderStatus.IN_PROGRESS
                        && (to == OrderStatus.COMPLETED || to == OrderStatus.CANCELLED));

        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid transition: " + serviceRequest.getStatus() + " -> " + to
            );
        }

        serviceRequest.setStatus(to);
        if (etaText != null && !etaText.isBlank()) serviceRequest.setEtaText(etaText);
        if (note != null && !note.isBlank()) serviceRequest.setNotes(note);
        serviceRequestRepository.save(serviceRequest);

        activityLogService.log(
                providerId,
                "ORDER_STATUS_UPDATE",
                "providerId=" + providerId + ", orderId=" + orderId + ", to=" + to
                        + (etaText != null && !etaText.isBlank() ? ", eta=" + etaText : "")
                        + (note != null && !note.isBlank() ? ", note=" + note : "")
        );
    }

    /* ========================= Helpers ========================= */

    private ServiceRequest requireOwnedByProvider(Long providerId, Long orderId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (serviceRequest.getProvider() == null
                || !Objects.equals(serviceRequest.getProvider().getProviderId(), providerId)) {
            throw new EntityNotFoundException("Not your order");
        }
        return serviceRequest;
    }

    private OrderSummaryDTO toSummary(ServiceRequest serviceRequest) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.setId(serviceRequest.getId());
        dto.setStatus(serviceRequest.getStatus().name());
        dto.setPickup(serviceRequest.getPickupAddress());     // from @Transient getter
        dto.setDropoff(serviceRequest.getDeliveryAddress());  // from @Transient getter
        dto.setScheduledAt(serviceRequest.getScheduledAt());
        dto.setTotalPrice(serviceRequest.getTotalPrice());

        String routeSummary =
                (serviceRequest.getDistanceKm() == null ? "" : serviceRequest.getDistanceKm() + " km")
                        + (serviceRequest.getDurationMin() == null ? "" : " / " + serviceRequest.getDurationMin() + " min");
        dto.setRouteSummary(routeSummary.trim());
        return dto;
    }

    private OrderDetailDTO toDetail(ServiceRequest serviceRequest, boolean maskPii) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setId(serviceRequest.getId());
        dto.setStatus(serviceRequest.getStatus().name());
        dto.setScheduledAt(serviceRequest.getScheduledAt());
        dto.setPickup(serviceRequest.getPickupAddress());
        dto.setDropoff(serviceRequest.getDeliveryAddress());
        dto.setDistanceKm(serviceRequest.getDistanceKm());
        dto.setDurationMin(serviceRequest.getDurationMin());
        dto.setTotalPrice(serviceRequest.getTotalPrice());
        dto.setNotes(serviceRequest.getNotes());
        dto.setEtaText(serviceRequest.getEtaText());
        dto.setRoutePolyline(serviceRequest.getRoutePolyline());

        String name = serviceRequest.getCustomerName();
        String phone = serviceRequest.getCustomerPhone();
        String email = serviceRequest.getCustomerEmail();
        dto.setCustomerNameMasked(maskPii ? maskString(name, 1, 1) : name);
        dto.setCustomerPhoneMasked(maskPii ? maskString(phone, 2, 2) : phone);
        dto.setCustomerEmailMasked(maskPii ? maskEmail(email) : email);

        return dto;
    }

    private String maskString(String value, int left, int right) {
        if (value == null || value.isBlank()) return "";
        if (value.length() <= left + right) return "*".repeat(Math.max(0, value.length()));
        return value.substring(0, left) + "****" + value.substring(value.length() - right);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        String[] parts = email.split("@", 2);
        return maskString(parts[0], 1, 1) + "@" + parts[1];
    }
}
