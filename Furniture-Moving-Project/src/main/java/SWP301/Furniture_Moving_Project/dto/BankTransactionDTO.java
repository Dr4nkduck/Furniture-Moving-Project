package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BankTransactionDTO {
    private String bankRef;       // Mã giao dịch ngân hàng
    private BigDecimal amount;    // Số tiền VND
    private String description;   // Nội dung chuyển khoản (chứa REQ123)
    private LocalDateTime txTime; // Thời gian giao dịch

    public String getBankRef() {
        return bankRef;
    }
    public void setBankRef(String bankRef) {
        this.bankRef = bankRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTxTime() {
        return txTime;
    }
    public void setTxTime(LocalDateTime txTime) {
        this.txTime = txTime;
    }
}
