package SWP301.Furniture_Moving_Project.dto;

public class ProviderOrderUpdateStatusDTO {
    private String status;       // pending/accepted/declined/in_progress/completed/cancelled
    private String cancelReason; // optional

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
