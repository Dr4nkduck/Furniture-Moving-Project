package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Integer contractId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String status; // pending | accepted

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    // ----- Getters & Setters -----
    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(OffsetDateTime signedAt) {
        this.signedAt = signedAt;
    }
}
