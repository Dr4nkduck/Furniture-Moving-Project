package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.DTO.AddressDTO;
import SWP301.Furniture_Moving_Project.model.Address;
import SWP301.Furniture_Moving_Project.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional
    public Integer create(AddressDTO dto) {
        Address a = new Address();
        a.setUserId(dto.getUserId());
        a.setAddressType(dto.getAddressType());
        a.setStreetAddress(dto.getStreetAddress());
        a.setCity(dto.getCity());
        a.setState(dto.getState());
        a.setZipCode(dto.getZipCode());
        a.setLatitude(dto.getLatitude());
        a.setLongitude(dto.getLongitude());
        a.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        addressRepository.save(a);
        return a.getAddressId();
    }
}
