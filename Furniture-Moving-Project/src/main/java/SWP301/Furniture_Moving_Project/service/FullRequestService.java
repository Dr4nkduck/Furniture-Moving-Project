package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.CreateFullRequestDTO;
import SWP301.Furniture_Moving_Project.dto.CreateServiceRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FullRequestService {

    private final AddressService addressService;
    private final ServiceRequestService serviceRequestService;

    public FullRequestService(AddressService addressService,
                              ServiceRequestService serviceRequestService) {
        this.addressService = addressService;
        this.serviceRequestService = serviceRequestService;
    }

    /**
     * Create a full request: create pickup & delivery addresses, then create the service request.
     * Returns the created ServiceRequest ID (Long).
     */
    @Transactional
    public Long createAll(CreateFullRequestDTO dto) {
        // 1) Create two addresses (AddressService currently returns Integer IDs)
        Integer pickupAddressId = addressService.create(dto.getPickupAddress());
        Integer deliveryAddressId = addressService.create(dto.getDeliveryAddress());

        // 2) Attach to the request DTO
        CreateServiceRequestDTO requestDto = dto.getRequest();
        requestDto.setPickupAddressId(pickupAddressId);
        requestDto.setDeliveryAddressId(deliveryAddressId);

        // 3) Create the service request (ServiceRequestService returns Long now)
        return serviceRequestService.create(requestDto);
    }
}
