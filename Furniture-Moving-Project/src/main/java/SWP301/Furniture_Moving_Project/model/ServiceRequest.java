package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_requests", schema = "dbo")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    // Provider được chỉ định (assigned provider)
    @Column(name = "provider_id")
    private Integer providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_address_id", referencedColumnName = "address_id", nullable = false)
    private Address pickupAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id", referencedColumnName = "address_id", nullable = false)
    private Address deliveryAddress;

    @Column(name = "request_date", nullable = false, columnDefinition = "datetime2")
    private LocalDateTime requestDate;

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "contract_id")
    private Integer contractId;

    @Column(name = "created_at", columnDefinition = "datetime2")
    private LocalDateTime createdAt;

    // ==== Hủy đơn ====

    @Column(name = "cancel_reason", columnDefinition = "NVARCHAR(500)")
    private String cancelReason;

    @Column(name = "cancelled_at", columnDefinition = "datetime2")
    private LocalDateTime cancelledAt;

    // NEW: ai là người hủy: CUSTOMER | PROVIDER | ADMIN
    @Column(name = "cancelled_by")
    private String cancelledBy;

    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FurnitureItem> furnitureItems = new ArrayList<>();

    // ==== Thanh toán ====

    @Column(name = "payment_status")
    private String paymentStatus; // ví dụ: PENDING | PAID | FAILED | EXPIRED

    @Column(name = "paid_at", columnDefinition = "datetime2")
    private LocalDateTime paidAt;

    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount; // nếu chọn DEPOSIT_20 thì lưu số tiền đặt cọc

    @Column(name = "payment_type")
    private String paymentType; // ví dụ: DEPOSIT_20 | FULL

    // ===== Constructor =====

    public ServiceRequest() {
    }

    @PrePersist
    public void prePersist() {
        if (requestDate == null) requestDate = LocalDateTime.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "pending";
    }

    // ========= Helper: rule hủy / yêu cầu hủy gom 1 chỗ =========

    /**
     * Customer có thể HỦY TRỰC TIẾP đơn không?
     *
     * Giai đoạn 1:
     *  - Cho phép khi: đơn còn đang chuẩn bị, CHƯA thanh toán xong:
     *      + status: không phải paid / in_progress / completed / cancelled
     *      + paymentStatus != PAID
     */
    @Transient
    public boolean isCancellableByCustomer() {
        if (status == null) return false;

        String st = status.trim().toLowerCase();

        // Không cho hủy nếu đã được xử lý / xong / đã hủy
        if ("paid".equals(st)
                || "in_progress".equals(st)
                || "completed".equals(st)
                || "cancelled".equals(st)) {
            return false;
        }

        // Nếu trạng thái thanh toán đã PAID (dù status khác) thì cũng không cho hủy trực tiếp
        if (paymentStatus != null && "paid".equalsIgnoreCase(paymentStatus.trim())) {
            return false;
        }

        // Còn lại (pending, ready_to_pay, ...) thì cho hủy
        return true;
    }

    /**
     * Customer có thể YÊU CẦU HỦY (sau khi đã thanh toán, chờ provider xét duyệt) không?
     *
     * Giai đoạn 2:
     *  - Cho phép khi:
     *      + Đơn đã thanh toán: status = paid hoặc paymentStatus = PAID
     *      + Và CHƯA in_progress / completed / cancelled
     */
    @Transient
    public boolean isCancellationRequestAllowedByCustomer() {
        if (status == null) return false;

        String st = status.trim().toLowerCase();

        // Không cho yêu cầu hủy nếu đơn đã đang làm / xong / đã hủy
        boolean alreadyInProgressOrDone =
                "in_progress".equals(st) ||
                "completed".equals(st) ||
                "cancelled".equals(st);

        if (alreadyInProgressOrDone) {
            return false;
        }

        boolean isPaidStatus = "paid".equals(st);
        boolean isPaidPayment = (paymentStatus != null)
                && "paid".equalsIgnoreCase(paymentStatus.trim());

        // Giai đoạn 2: đã paid nhưng chưa in_progress/completed/cancelled
        return isPaidStatus || isPaidPayment;
    }

    // ===== Getters & Setters =====

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    public Address getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(Address pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public List<FurnitureItem> getFurnitureItems() {
        return furnitureItems;
    }

    public void setFurnitureItems(List<FurnitureItem> furnitureItems) {
        this.furnitureItems = furnitureItems;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }
}
