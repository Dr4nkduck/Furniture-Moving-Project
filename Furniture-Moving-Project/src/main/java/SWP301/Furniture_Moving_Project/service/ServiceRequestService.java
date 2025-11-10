package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.CreateServiceRequestDTO;
import SWP301.Furniture_Moving_Project.dto.FurnitureItemDTO;
import SWP301.Furniture_Moving_Project.model.Address;
import SWP301.Furniture_Moving_Project.model.Customer;
import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.AddressRepository;
import SWP301.Furniture_Moving_Project.repository.CustomerRepository;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    public Integer create(CreateServiceRequestDTO dto) {

        // 1) Validate provider (nếu có)
        if (dto.getProviderId() != null && !providerRepository.existsById(dto.getProviderId())) {
            throw new IllegalArgumentException("Provider not found: id=" + dto.getProviderId());
        }

        // 2) Lấy customer -> userId để kiểm tra sở hữu địa chỉ
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: id=" + dto.getCustomerId()));
        Integer expectedUserId = customer.getUser().getUserId();

        // 3) Tìm địa chỉ
        Address pickup = addressRepository.findById(dto.getPickupAddressId())
                .orElseThrow(() -> new IllegalArgumentException("Pickup address not found: id=" + dto.getPickupAddressId()));
        Address delivery = addressRepository.findById(dto.getDeliveryAddressId())
                .orElseThrow(() -> new IllegalArgumentException("Delivery address not found: id=" + dto.getDeliveryAddressId()));

        // 4) Kiểm tra ownership: addresses.user_id == customers.user_id
        if (!Objects.equals(pickup.getUserId(), expectedUserId) || !Objects.equals(delivery.getUserId(), expectedUserId)) {
            throw new IllegalArgumentException("Addresses do not belong to this user's account");
        }

        // 5) Tạo ServiceRequest
        ServiceRequest sr = new ServiceRequest();
        sr.setCustomerId(dto.getCustomerId());
        sr.setProviderId(dto.getProviderId());
        sr.setPreferredDate(dto.getPreferredDate());
//        sr.setStatus(dto.getStatus() == null || dto.getStatus().isBlank() ? "pending" : dto.getStatus());
        sr.setTotalCost(dto.getTotalCost());

        // set ManyToOne addresses (entity đã managed)
        sr.setPickupAddress(pickup);
        sr.setDeliveryAddress(delivery);

        // 6) Map items (nếu có)
        List<FurnitureItem> items = new ArrayList<>();
        if (dto.getFurnitureItems() != null) {
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
        }
        sr.setFurnitureItems(items);

        // 7) Lưu & trả id
        final ServiceRequest saved = serviceRequestRepository.save(sr);
        return saved.getRequestId();
    }
}
