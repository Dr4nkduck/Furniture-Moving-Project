package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.CancellationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CancellationRequestRepository
        extends JpaRepository<CancellationRequest, Integer> {

    boolean existsByServiceRequestIdAndStatus(Integer serviceRequestId, String status);

    Optional<CancellationRequest> findFirstByServiceRequestIdAndStatusOrderByCreatedAtDesc(
            Integer serviceRequestId,
            String status
    );

    // ✅ Lấy toàn bộ yêu cầu hủy của 1 provider (mọi trạng thái)
    List<CancellationRequest> findByProviderIdOrderByCreatedAtDesc(Integer providerId);

    // ✅ Lấy theo provider + status (requested / approved / rejected)
    List<CancellationRequest> findByProviderIdAndStatusOrderByCreatedAtDesc(
            Integer providerId,
            String status
            
    );
        Optional<CancellationRequest> findTopByServiceRequestIdAndProviderIdOrderByCreatedAtDesc(
            Integer serviceRequestId,
            Integer providerId
    );
}
