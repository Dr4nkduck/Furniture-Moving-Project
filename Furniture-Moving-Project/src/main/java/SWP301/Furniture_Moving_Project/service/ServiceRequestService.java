package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.CreateServiceRequestDTO;
import SWP301.Furniture_Moving_Project.dto.FurnitureItemDTO;
import SWP301.Furniture_Moving_Project.model.Address;
import SWP301.Furniture_Moving_Project.model.Customer;
import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.model.OrderStatus;
import SWP301.Furniture_Moving_Project.repository.AddressRepository;
import SWP301.Furniture_Moving_Project.repository.CustomerRepository;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final AddressRepository addressRepository;
    private final ProviderRepository providerRepository;
    private final CustomerRepository customerRepository;

    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository,
                                 AddressRepository addressRepository,
                                 ProviderRepository providerRepository,
                                 CustomerRepository customerRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.addressRepository = addressRepository;
        this.providerRepository = providerRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Long create(CreateServiceRequestDTO dto) {

        // ---- Normalize IDs from DTO (Integer -> Long) ----
        Long customerId = dto.getCustomerId() != null ? dto.getCustomerId().longValue() : null;
        Long providerId = dto.getProviderId() != null ? dto.getProviderId().longValue() : null;
        Long pickupAddressId = dto.getPickupAddressId() != null ? dto.getPickupAddressId().longValue() : null;
        Long deliveryAddressId = dto.getDeliveryAddressId() != null ? dto.getDeliveryAddressId().longValue() : null;

        if (customerId == null) {
            throw new IllegalArgumentException("customerId is required");
        }

        // ---- Validate provider (optional) ----
        Provider provider = null;
        if (providerId != null) {
            boolean exists = providerRepository.existsById(providerId);
            if (!exists) {
                throw new IllegalArgumentException("Provider not found: id=" + providerId);
            }
            provider = providerRepository.getReferenceById(providerId);
        }

        // ---- Load addresses ----
        Address pickup = addressRepository.findById(pickupAddressId)
                .orElseThrow(() -> new IllegalArgumentException("Pickup address not found: id=" + pickupAddressId));
        Address delivery = addressRepository.findById(deliveryAddressId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery address not found: id=" + deliveryAddressId));

        // (Optional) Ownership rule: both addresses must belong to the same customer
        if (pickup.getUserId() == null || delivery.getUserId() == null
                || !pickup.getUserId().equals(customerId)
                || !delivery.getUserId().equals(customerId)) {
            throw new IllegalArgumentException("Addresses do not belong to this customer");
        }

        // ---- Load customer reference ----
        Customer customer = customerRepository.getReferenceById(customerId);

        // ---- Build entity according to the new model ----
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setCustomer(customer);
        serviceRequest.setProvider(provider);
        serviceRequest.setPreferredDate(dto.getPreferredDate());

        // Status: map from DTO string (if provided) to enum; default PENDING_OFFER
        OrderStatus status = dto.getStatus() == null || dto.getStatus().isBlank()
                ? OrderStatus.PENDING_OFFER
                : OrderStatus.safeParse(dto.getStatus());
        if (status == null) status = OrderStatus.PENDING_OFFER;
        serviceRequest.setStatus(status);

        // Prices/addresses
        serviceRequest.setTotalPrice(dto.getTotalCost());
        serviceRequest.setPickupAddressEntity(pickup);
        serviceRequest.setDeliveryAddressEntity(delivery);

        // ---- Map furniture items ----
        List<FurnitureItem> items = new ArrayList<>();
        if (dto.getFurnitureItems() != null) {
            for (FurnitureItemDTO f : dto.getFurnitureItems()) {
                FurnitureItem item = new FurnitureItem();
                item.setServiceRequest(serviceRequest);
                item.setItemType(f.getItemType());
                item.setSize(f.getSize());
                item.setQuantity(f.getQuantity());
                item.setIsFragile(f.getIsFragile());
                item.setSpecialHandling(f.getSpecialHandling());
                items.add(item);
            }
        }
        serviceRequest.setFurnitureItems(items);

        // ---- Persist ----
        serviceRequestRepository.save(serviceRequest);
        return serviceRequest.getId();
    }
}
