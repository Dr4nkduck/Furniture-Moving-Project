    package SWP301.Furniture_Moving_Project.dto;

    import jakarta.validation.Valid;
    import jakarta.validation.constraints.*;
    import java.math.BigDecimal;
    import java.util.List;

    public class CreateFullRequestDTO {

        @NotNull @Valid
        private AddressPayload pickupAddress;

        @NotNull @Valid
        private AddressPayload deliveryAddress;

        @NotNull @Valid
        private RequestMetaDTO request;   // ✅ KHÔNG dùng CreateServiceRequestDTO nữa

        @NotEmpty @Valid
        private List<FurnitureItemPayload> furnitureItems;

        // getters/setters
        public AddressPayload getPickupAddress() { return pickupAddress; }
        public void setPickupAddress(AddressPayload pickupAddress) { this.pickupAddress = pickupAddress; }
        public AddressPayload getDeliveryAddress() { return deliveryAddress; }
        public void setDeliveryAddress(AddressPayload deliveryAddress) { this.deliveryAddress = deliveryAddress; }
        public RequestMetaDTO getRequest() { return request; }
        public void setRequest(RequestMetaDTO request) { this.request = request; }
        public List<FurnitureItemPayload> getFurnitureItems() { return furnitureItems; }
        public void setFurnitureItems(List<FurnitureItemPayload> furnitureItems) { this.furnitureItems = furnitureItems; }

        // ----- Inner classes -----
        public static class AddressPayload {
            @NotBlank private String addressLine1;
            private String district;
            private String city;
            private Integer floor;
            private Boolean hasElevator;
            private String contactName;
            private String contactPhone;
            private String specialInstructions;

            public String getAddressLine1() { return addressLine1; }
            public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
            public String getDistrict() { return district; }
            public void setDistrict(String district) { this.district = district; }
            public String getCity() { return city; }
            public void setCity(String city) { this.city = city; }
            public Integer getFloor() { return floor; }
            public void setFloor(Integer floor) { this.floor = floor; }
            public Boolean getHasElevator() { return hasElevator; }
            public void setHasElevator(Boolean hasElevator) { this.hasElevator = hasElevator; }
            public String getContactName() { return contactName; }
            public void setContactName(String contactName) { this.contactName = contactName; }
            public String getContactPhone() { return contactPhone; }
            public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
            public String getSpecialInstructions() { return specialInstructions; }
            public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
        }

        public static class FurnitureItemPayload {
            @NotBlank private String name;
            @Min(1)  private Integer quantity;
            private Integer lengthCm, widthCm, heightCm;
            private BigDecimal weightKg;
            private Boolean fragile;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public Integer getQuantity() { return quantity; }
            public void setQuantity(Integer quantity) { this.quantity = quantity; }
            public Integer getLengthCm() { return lengthCm; }
            public void setLengthCm(Integer lengthCm) { this.lengthCm = lengthCm; }
            public Integer getWidthCm() { return widthCm; }
            public void setWidthCm(Integer widthCm) { this.widthCm = widthCm; }
            public Integer getHeightCm() { return heightCm; }
            public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }
            public BigDecimal getWeightKg() { return weightKg; }
            public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
            public Boolean getFragile() { return fragile; }
            public void setFragile(Boolean fragile) { this.fragile = fragile; }
        }
    }
