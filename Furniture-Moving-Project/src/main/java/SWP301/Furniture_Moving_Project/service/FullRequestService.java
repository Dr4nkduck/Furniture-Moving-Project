package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.AddressDTO;
import SWP301.Furniture_Moving_Project.dto.CreateFullRequestDTO;
import SWP301.Furniture_Moving_Project.dto.CreateServiceRequestDTO;
import SWP301.Furniture_Moving_Project.model.Address;
import SWP301.Furniture_Moving_Project.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FullRequestService {

    private final AddressService addressService;
    private final ServiceRequestService serviceRequestService;
    private final AddressRepository addressRepository;

    public FullRequestService(AddressService addressService,
                              ServiceRequestService serviceRequestService,
                              AddressRepository addressRepository) {
        this.addressService = addressService;
        this.serviceRequestService = serviceRequestService;
        this.addressRepository = addressRepository;
    }

    @Transactional
    public Integer createAll(CreateFullRequestDTO dto) {
        // 1) tạo 2 address
        Integer pickupId = addressService.create(dto.getPickupAddress());
        Integer deliveryId = addressService.create(dto.getDeliveryAddress());

        // 2) gắn vào request DTO rồi gọi service hiện có
        CreateServiceRequestDTO req = dto.getRequest();
        req.setPickupAddressId(pickupId);
        req.setDeliveryAddressId(deliveryId);

        // 3) tạo request
        return serviceRequestService.create(req);
    }
}
