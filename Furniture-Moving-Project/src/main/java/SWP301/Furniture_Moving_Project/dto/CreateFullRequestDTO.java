package SWP301.Furniture_Moving_Project.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class CreateFullRequestDTO {

    @NotNull
    @Valid
    private AddressDTO pickupAddress;

    @NotNull
    @Valid
    private AddressDTO deliveryAddress;

    @NotNull
    @Valid
    private CreateServiceRequestDTO request;

    public AddressDTO getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(AddressDTO pickupAddress) { this.pickupAddress = pickupAddress; }
    public AddressDTO getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(AddressDTO deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public CreateServiceRequestDTO getRequest() { return request; }
    public void setRequest(CreateServiceRequestDTO request) { this.request = request; }
}
