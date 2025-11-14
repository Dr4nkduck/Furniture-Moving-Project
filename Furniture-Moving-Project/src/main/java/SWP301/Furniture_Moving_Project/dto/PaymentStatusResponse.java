package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
// dùng LocalDateTime thay vì OffsetDateTime
import java.time.LocalDateTime;

public class PaymentStatusResponse {

    private String status;
    private BigDecimal amount;
    private LocalDateTime paidAt;  // 🔁 đổi sang LocalDateTime
    private String paymentType;

    public PaymentStatusResponse() {}

    public PaymentStatusResponse(String status, BigDecimal amount,
                                 LocalDateTime paidAt, String paymentType) {
        this.status = status;
        this.amount = amount;
        this.paidAt = paidAt;
        this.paymentType = paymentType;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
}
