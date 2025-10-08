package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_addresses")
public class RequestAddress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_address_id")
    private Integer requestAddressId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest serviceRequest;
    
    @Column(name = "address_type", nullable = false, length = 20)
    private String addressType;
    
    @Column(name = "street_address", nullable = false)
    private String streetAddress;
    
    @Column(name = "city", nullable = false, length = 100)
    private String city;
    
    @Column(name = "state", nullable = false, length = 50)
    private String state;
    
    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;
    
    @Column(name = "contact_name", length = 100)
    private String contactName;
    
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;
    
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public RequestAddress() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getRequestAddressId() {
        return requestAddressId;
    }
    
    public void setRequestAddressId(Integer requestAddressId) {
        this.requestAddressId = requestAddressId;
    }
    
    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }
    
    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }
    
    public String getAddressType() {
        return addressType;
    }
    
    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }
    
    public String getStreetAddress() {
        return streetAddress;
    }
    
    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
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
    
    public String getSpecialInstructions() {
        return specialInstructions;
    }
    
    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}