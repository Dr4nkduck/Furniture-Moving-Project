package SWP301.Furniture_Moving_Project.controller.dto;

import SWP301.Furniture_Moving_Project.model.AccountStatus;

public class ChangeStatusRequest {
    private AccountStatus status;
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
}
