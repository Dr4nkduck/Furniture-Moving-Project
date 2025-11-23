package SWP301.Furniture_Moving_Project.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * AddressDTO dành cho quản lý sổ địa chỉ của user.
 * Không dùng DTO này trong CreateFullRequestDTO vì /full không cần userId từ FE.
 */
public class AddressDTO {

    @NotNull(message = "userId is required")
    private Integer userId;

    @NotBlank(message = "addressType is required")
    private String addressType; // "home"/"office"/"warehouse"/...

    @NotBlank(message = "streetAddress is required")
    private String streetAddress;

    @NotBlank(message = "city is required")
    private String city;

    @NotBlank(message = "state is required")
    private String state;

    @NotBlank(message = "zipCode is required")
    private String zipCode;

    // Optional
    @Digits(integer = 10, fraction = 8)
    private BigDecimal latitude;

    @Digits(integer = 11, fraction = 8)
    private BigDecimal longitude;

    private Boolean isDefault = false;

    // Getters / Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getAddressType() { return addressType; }
    public void setAddressType(String addressType) { this.addressType = addressType; }
    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
