package SWP301.Furniture_Moving_Project.DTO;

public class ServiceRequestResponseDTO {

    private Integer requestId;

    public ServiceRequestResponseDTO() {
    }

    public ServiceRequestResponseDTO(Integer requestId) {
        this.requestId = requestId;
    }

    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }
}
