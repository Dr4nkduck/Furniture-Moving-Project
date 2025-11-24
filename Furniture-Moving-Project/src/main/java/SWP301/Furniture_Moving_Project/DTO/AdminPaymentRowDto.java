package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

import SWP301.Furniture_Moving_Project.model.Payment;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;

public class AdminPaymentRowDto {

    // ServiceRequest.requestId trong project của bạn là Integer
    private Integer requestId;
    private String paymentRef;

    // Có thể để trống (sẽ hiển thị "—" bên JS)
    private String customerName;
    private String providerName;

    // pending / ready_to_pay / paid / in_progress / completed / cancelled / declined ...
    private String requestStatus;

    // PENDING / PAID (simple mapping, không dùng phương thức Payment.getPaymentStatus())
    private String paymentStatus;

    private BigDecimal amount;    // số tiền để admin đối chiếu
    private String paymentType;   // FULL / DEPOSIT ... (hiện tại chưa đọc từ entity, có thể null)

    private String requestDate;   // string để dễ serialize ra JSON
    private String preferredDate;

    /* ================== STATIC FACTORY ================== */

    public static AdminPaymentRowDto from(ServiceRequest r, Payment p) {
        AdminPaymentRowDto dto = new AdminPaymentRowDto();
        if (r == null) {
            return dto;
        }

        // ID + mã thanh toán
        dto.requestId = r.getRequestId();     // <- Integer, không còn lỗi Long
        dto.paymentRef = "REQ" + r.getRequestId();

        // Nếu ServiceRequest của bạn có sẵn tên KH / Provider dạng String,
        // bạn có thể UNCOMMENT 2 dòng dưới (và sửa tên getter cho đúng):
        //
        // dto.customerName = r.getCustomerName();
        // dto.providerName = r.getProviderName();

        // Trạng thái request (giữ nguyên code từ entity)
        dto.requestStatus = r.getStatus();

        // Số tiền: ưu tiên Payment.amount, nếu null thì lấy totalCost của request
        if (p != null && p.getAmount() != null) {
            dto.amount = p.getAmount();
        } else {
            dto.amount = r.getTotalCost();
        }

        // Simple rule: nếu trạng thái đã "paid" hoặc sau đó, coi như thanh toán = PAID
        String s = r.getStatus();
        if (s != null) {
            switch (s) {
                case "paid":
                case "in_progress":
                case "completed":
                    dto.paymentStatus = "PAID";
                    break;
                default:
                    dto.paymentStatus = "PENDING";
            }
        } else {
            dto.paymentStatus = "PENDING";
        }

        // Nếu Payment của bạn có field / getter paymentType (enum hoặc String),
        // bạn có thể gán vào đây. Tạm thời để null để tránh lỗi “method undefined”.
        //
        // if (p != null && p.getPaymentType() != null) {
        //     dto.paymentType = p.getPaymentType().toString();
        // }

        // Format ngày ra String cho nhẹ nhàng bên JSON
        if (r.getRequestDate() != null) {
            dto.requestDate = r.getRequestDate().toString();
        }
        if (r.getPreferredDate() != null) {
            dto.preferredDate = r.getPreferredDate().toString();
        }

        return dto;
    }

    /* ================== GETTERS / SETTERS ================== */

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public String getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(String preferredDate) {
        this.preferredDate = preferredDate;
    }
}
