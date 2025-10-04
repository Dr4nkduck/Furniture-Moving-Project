package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "request_addresses")
public class RequestAddress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @JsonIgnore
    private ServiceRequest request;
    
    @Column(name = "address_type", length = 10, nullable = false)
    private String addressType;
    
    @Column(name = "address_line", nullable = false)
    private String addressLine;
    
    @Column(name = "district", length = 100)
    private String district;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "contact_name", length = 100)
    private String contactName;
    
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    // Constructors
    public RequestAddress() {
    }

    public RequestAddress(ServiceRequest request, String addressType, String addressLine) {
        this.request = request;
        this.addressType = addressType;
        this.addressLine = addressLine;
    }

    // Getters and Setters
    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public ServiceRequest getRequest() {
        return request;
    }

    public void setRequest(ServiceRequest request) {
        this.request = request;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}