package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.QuickRequestDTO;
import SWP301.Furniture_Moving_Project.model.Address;
import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.AddressRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ServiceRequestAppService {

    private final ServiceRequestRepository requestRepo;
    private final AddressRepository addressRepo;

    public ServiceRequestAppService(ServiceRequestRepository requestRepo,
                                    AddressRepository addressRepo) {
        this.requestRepo = requestRepo;
        this.addressRepo = addressRepo;
    }

    @Transactional
    public Integer createPending(QuickRequestDTO dto) {
        // lấy proxy theo id, không query full object
        Address pickup   = addressRepo.getReferenceById(dto.pickupAddressId());
        Address delivery = addressRepo.getReferenceById(dto.deliveryAddressId());

        ServiceRequest sr = new ServiceRequest();
        sr.setCustomerId(dto.customerId());
        sr.setProviderId(dto.providerId());                         // có thể null
        sr.setPickupAddress(pickup);
        sr.setDeliveryAddress(delivery);
        sr.setPreferredDate(LocalDate.parse(dto.preferredDate()));  // yyyy-MM-dd
        sr.setTotalCost(dto.totalCost());
        sr.setStatus("pending");                                     // đảm bảo pending (dù @PrePersist cũng set)

        // (tuỳ chọn) thêm các item nếu FE gửi
        if (dto.furnitureItems() != null) {
            dto.furnitureItems().forEach(it -> {
                FurnitureItem fi = new FurnitureItem();
                fi.setServiceRequest(sr);
                fi.setItemType(it.itemType());
                fi.setSize(it.size());
                fi.setQuantity(it.quantity() == null ? 1 : it.quantity());
                fi.setIsFragile(Boolean.TRUE.equals(it.isFragile()));
                fi.setSpecialHandling(it.specialHandling());
                sr.getFurnitureItems().add(fi); // cascade ALL sẽ tự lưu
            });
        }

        return requestRepo.save(sr).getRequestId();
    }
}

