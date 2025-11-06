package SWP301.Furniture_Moving_Project.dto;

public class ProviderOrderStatusUpdateDTO {
    private String status; // in_progress / completed / cancelled

    public ProviderOrderStatusUpdateDTO() {}
    public ProviderOrderStatusUpdateDTO(String status) { this.status = status; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
