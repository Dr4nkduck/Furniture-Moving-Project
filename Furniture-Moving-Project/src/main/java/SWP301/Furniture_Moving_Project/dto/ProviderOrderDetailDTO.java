package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ProviderOrderDetailDTO {

    // ===== Thông tin đơn cơ bản =====
    private Integer requestId;
    private String status;
    private LocalDateTime requestDate;
    private LocalDate preferredDate;

    // ===== Thông tin khách hàng =====
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // ===== Địa chỉ =====
    private String pickupFull;
    private String deliveryFull;

    // ===== Chi phí =====
    private BigDecimal totalCostEstimate;

    // ===== Danh sách item =====
    private List<ProviderOrderItemDTO> items;

    // ===== Thông tin thanh toán (nếu cần cho UI sau này) =====
    private String paymentStatus; // PENDING / PAID / FAILED ...
    private String paymentType;   // DEPOSIT / FULL_PAYMENT / null

    // ===== Hủy trực tiếp (giai đoạn 1) =====
    /**
     * Lý do hủy được lưu trực tiếp trên service_requests.cancel_reason
     * (dùng cho case KH chưa thanh toán và tự hủy đơn).
     */
    private String cancelReason;

    // ===== YÊU CẦU HỦY (giai đoạn 2 + 3) =====
    /**
     * ID của bản ghi trong bảng cancellation_requests (nếu có).
     */
    private Integer cancellationId;

    /**
     * Trạng thái yêu cầu hủy:
     *  - requested
     *  - approved
     *  - rejected
     *  hoặc null nếu chưa từng có yêu cầu hủy.
     */
    private String cancellationStatus;

    /**
     * Lý do khách nhập khi gửi yêu cầu hủy (cancellation_requests.reason).
     */
    private String cancellationReason;

    /**
     * Ghi chú quyết định của provider khi APPROVE/REJECT
     * (cancellation_requests.decision_note).
     */
    private String cancellationDecisionNote;

    // ================== GETTER / SETTER ==================

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getPickupFull() {
        return pickupFull;
    }

    public void setPickupFull(String pickupFull) {
        this.pickupFull = pickupFull;
    }

    public String getDeliveryFull() {
        return deliveryFull;
    }

    public void setDeliveryFull(String deliveryFull) {
        this.deliveryFull = deliveryFull;
    }

    public BigDecimal getTotalCostEstimate() {
        return totalCostEstimate;
    }

    public void setTotalCostEstimate(BigDecimal totalCostEstimate) {
        this.totalCostEstimate = totalCostEstimate;
    }

    public List<ProviderOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ProviderOrderItemDTO> items) {
        this.items = items;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Integer getCancellationId() {
        return cancellationId;
    }

    public void setCancellationId(Integer cancellationId) {
        this.cancellationId = cancellationId;
    }

    public String getCancellationStatus() {
        return cancellationStatus;
    }

    public void setCancellationStatus(String cancellationStatus) {
        this.cancellationStatus = cancellationStatus;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCancellationDecisionNote() {
        return cancellationDecisionNote;
    }

    public void setCancellationDecisionNote(String cancellationDecisionNote) {
        this.cancellationDecisionNote = cancellationDecisionNote;
    }
}
