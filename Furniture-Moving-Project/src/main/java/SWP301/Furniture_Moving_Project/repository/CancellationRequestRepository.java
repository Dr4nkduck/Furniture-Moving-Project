package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.CancellationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CancellationRequestRepository
        extends JpaRepository<CancellationRequest, Integer> {

    // Check xem đơn này đã có yêu cầu hủy với status cụ thể chưa (requested / approved / rejected)
    boolean existsByServiceRequestIdAndStatus(Integer serviceRequestId, String status);

    // Lấy yêu cầu hủy mới nhất theo serviceRequestId + status (nếu cần)
    Optional<CancellationRequest> findFirstByServiceRequestIdAndStatusOrderByCreatedAtDesc(
            Integer serviceRequestId,
            String status
    );

    // ✅ Bên provider: lấy yêu cầu hủy mới nhất của đơn này cho provider này
    Optional<CancellationRequest> findTopByServiceRequestIdAndProviderIdOrderByCreatedAtDesc(
            Integer serviceRequestId,
            Integer providerId
    );

    // ✅ Bên customer: lấy yêu cầu hủy mới nhất của đơn này (bất kể provider nào)
    Optional<CancellationRequest> findTopByServiceRequestIdOrderByCreatedAtDesc(
            Integer serviceRequestId
    );

    // ✅ Lấy toàn bộ yêu cầu hủy của 1 provider (mọi trạng thái)
    List<CancellationRequest> findByProviderIdOrderByCreatedAtDesc(Integer providerId);

    // ✅ Lấy theo provider + status (requested / approved / rejected)
    List<CancellationRequest> findByProviderIdAndStatusOrderByCreatedAtDesc(
            Integer providerId,
            String status
    );
}
        