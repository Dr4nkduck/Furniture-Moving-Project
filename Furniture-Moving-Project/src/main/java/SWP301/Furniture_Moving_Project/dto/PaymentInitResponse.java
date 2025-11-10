package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PaymentInitResponse {
    public String mode = "VIETQR";
    public String vietqrImageUrl;

    public String bankCode;
    public String accountNumber;
    public String accountName;
    public String addInfo;

    public BigDecimal amount;
    public OffsetDateTime expireAt;

    public PaymentInitResponse() {}

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getVietqrImageUrl() { return vietqrImageUrl; }
    public void setVietqrImageUrl(String vietqrImageUrl) { this.vietqrImageUrl = vietqrImageUrl; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAddInfo() { return addInfo; }
    public void setAddInfo(String addInfo) { this.addInfo = addInfo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public OffsetDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }
}
