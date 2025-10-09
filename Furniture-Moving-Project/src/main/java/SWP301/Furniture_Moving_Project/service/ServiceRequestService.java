package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.CreateServiceRequestDTO;
import SWP301.Furniture_Moving_Project.dto.FurnitureItemDTO;
import SWP301.Furniture_Moving_Project.model.Address;
import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.AddressRepository;
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

    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository,
                                 AddressRepository addressRepository,
                                 ProviderRepository providerRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.addressRepository = addressRepository;
        this.providerRepository = providerRepository;
    }

    @Transactional
    public Integer create(CreateServiceRequestDTO dto) {

        // Provider tồn tại (nếu có)
        if (dto.getProviderId() != null) {
            boolean exists = providerRepository.existsById(dto.getProviderId());
            if (!exists) {
                throw new IllegalArgumentException("Provider not found: id=" + dto.getProviderId());
            }
        }

        // Kiểm tra địa chỉ tồn tại
        Address pickup = addressRepository.findById(dto.getPickupAddressId())
                .orElseThrow(() -> new IllegalArgumentException("Pickup address not found: id=" + dto.getPickupAddressId()));
        Address delivery = addressRepository.findById(dto.getDeliveryAddressId())
                .orElseThrow(() -> new IllegalArgumentException("Delivery address not found: id=" + dto.getDeliveryAddressId()));

        // (Khuyến nghị) kiểm tra ownership cùng user (nếu đây là rule của bạn)
        if (!pickup.getUserId().equals(dto.getCustomerId()) || !delivery.getUserId().equals(dto.getCustomerId())) {
            throw new IllegalArgumentException("Addresses do not belong to this customer");
        }

        ServiceRequest sr = new ServiceRequest();
        sr.setCustomerId(dto.getCustomerId());
        sr.setProviderId(dto.getProviderId());
        sr.setPreferredDate(dto.getPreferredDate());
        sr.setStatus(dto.getStatus() == null || dto.getStatus().isBlank() ? "pending" : dto.getStatus());
        sr.setTotalCost(dto.getTotalCost());

        // set quan hệ address (entity managed)
        sr.setPickupAddress(pickup);
        sr.setDeliveryAddress(delivery);

        // Map items
        List<FurnitureItem> items = new ArrayList<>();
        for (FurnitureItemDTO f : dto.getFurnitureItems()) {
            FurnitureItem it = new FurnitureItem();
            it.setServiceRequest(sr);
            it.setItemType(f.getItemType());
            it.setSize(f.getSize());
            it.setQuantity(f.getQuantity());
            it.setIsFragile(f.getIsFragile());
            it.setSpecialHandling(f.getSpecialHandling());
            items.add(it);
        }
        sr.setFurnitureItems(items);

        serviceRequestRepository.save(sr);
        return sr.getRequestId();
    }
}
