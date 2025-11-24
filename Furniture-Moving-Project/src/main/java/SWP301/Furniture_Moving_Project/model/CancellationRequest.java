package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellation_requests")
public class CancellationRequest {

    // ===== Status constants (tránh hard-code string lung tung) =====
    public static final String STATUS_REQUESTED = "requested";
    public static final String STATUS_APPROVED  = "approved";
    public static final String STATUS_REJECTED  = "rejected";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cancellation_id")
    private Integer cancellationId;

    @Column(name = "service_request_id", nullable = false)
    private Integer serviceRequestId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "provider_id")
    private Integer providerId;

    @Column(name = "reason", columnDefinition = "NVARCHAR(500)")
    private String reason;

    @Column(name = "status", length = 20, nullable = false)
    private String status; // requested / approved / rejected (flex, không constraint DB)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decided_by")
    private Integer decidedBy; // user_id của người xử lý (provider/admin)

    @Column(name = "decision_note", columnDefinition = "NVARCHAR(500)")
    private String decisionNote;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = STATUS_REQUESTED; // default theo convention, không fix ở DB
        }
    }

    // ===== GETTERS & SETTERS =====
    public Integer getCancellationId() {
        return cancellationId;
    }

    public void setCancellationId(Integer cancellationId) {
        this.cancellationId = cancellationId;
    }

    public Integer getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Integer serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Integer getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(Integer decidedBy) {
        this.decidedBy = decidedBy;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }
}
