package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PaymentInitResponse {

    // Chế độ thanh toán (ví dụ: VIETQR, VNPAY...)
    private String mode = "VIETQR";

    // Ảnh VietQR (từ PayOS)
    private String vietqrImageUrl;

    // Thông tin tài khoản (nếu muốn hiển thị thêm cho user)
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String addInfo;

    // Số tiền cần thanh toán
    private BigDecimal amount;

    // Thời điểm hết hạn phiên thanh toán
    private OffsetDateTime expireAt;

    // Link checkout (mở trang PayOS nếu muốn)
    private String payUrl;

    // Mã tham chiếu giao dịch / orderCode
    private String txnRef;

    public PaymentInitResponse() {
    }

    // ==== getters / setters ====
    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getVietqrImageUrl() {
        return vietqrImageUrl;
    }
    public void setVietqrImageUrl(String vietqrImageUrl) {
        this.vietqrImageUrl = vietqrImageUrl;
    }

    public String getBankCode() {
        return bankCode;
    }
    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAddInfo() {
        return addInfo;
    }
    public void setAddInfo(String addInfo) {
        this.addInfo = addInfo;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public OffsetDateTime getExpireAt() {
        return expireAt;
    }
    public void setExpireAt(OffsetDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public String getPayUrl() {
        return payUrl;
    }
    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }

    public String getTxnRef() {
        return txnRef;
    }
    public void setTxnRef(String txnRef) {
        this.txnRef = txnRef;
    }
}
