package SWP301.Furniture_Moving_Project.dto;

import lombok.Data;

@Data
public class ContractDTO {
    private Integer contractId;
    private Integer customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String status;
    private String pickupAddress;
    private String deliveryAddress;
    private String preferredDate;
    private Double totalCost;
    private Integer requestId;
}
