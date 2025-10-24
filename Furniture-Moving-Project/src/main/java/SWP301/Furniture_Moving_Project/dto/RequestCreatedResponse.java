package SWP301.Furniture_Moving_Project.dto;

public class RequestCreatedResponse {
    private Integer requestId;
    private String status;

    public RequestCreatedResponse(Integer requestId, String status) {
        this.requestId = requestId;
        this.status = status;
    }

    public Integer getRequestId() { return requestId; }
    public String getStatus() { return status; }
}
