package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PaymentStatusResponse {
    private String status;
    private BigDecimal amount;
    private OffsetDateTime paidAt;

    public PaymentStatusResponse() {}

    public PaymentStatusResponse(String status, BigDecimal amount, OffsetDateTime paidAt) {
        this.status = status;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
}
