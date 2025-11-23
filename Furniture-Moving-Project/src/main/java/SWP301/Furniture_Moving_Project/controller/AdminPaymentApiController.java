package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.AdminPaymentRowDto;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * API cho Admin xác nhận thanh toán.
 * - GET  /api/admin/payments              : liệt kê các đơn để đối soát
 * - PUT  /api/admin/payments/{id}/confirm : admin bấm "Đã nhận được tiền"
 *
 * Ở bản đơn giản này, mình chỉ dùng ServiceRequest.
 * paymentStatus sẽ được suy ra từ request.status trong AdminPaymentRowDto.
 */
@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentApiController {

    private final ServiceRequestRepository serviceRequests;

    public AdminPaymentApiController(ServiceRequestRepository serviceRequests) {
        this.serviceRequests = serviceRequests;
    }

    // ================== LIST ĐƠN CHO MÀN "XÁC NHẬN THANH TOÁN" ==================
    @GetMapping
    public List<AdminPaymentRowDto> list() {
        // Các trạng thái admin quan tâm
        Set<String> allowedStatuses = new HashSet<>(
                Arrays.asList("ready_to_pay", "paid", "in_progress", "completed")
        );

        // Lấy tất cả request rồi lọc bằng Java (không cần custom query)
        List<ServiceRequest> allReqs = new ArrayList<>();
        serviceRequests.findAll().forEach(allReqs::add);

        return allReqs.stream()
                .filter(r -> r.getStatus() != null && allowedStatuses.contains(r.getStatus()))
                // Payment = null, AdminPaymentRowDto sẽ tự suy ra paymentStatus từ status
                .map(r -> AdminPaymentRowDto.from(r, null))
                .collect(Collectors.toList());
    }

    // ================== ADMIN BẤM "ĐÃ NHẬN ĐƯỢC TIỀN" ==================
    @PutMapping("/{requestId}/confirm")
    public AdminPaymentRowDto confirmPaid(@PathVariable Integer requestId) {

        ServiceRequest req = serviceRequests.findById(requestId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy đơn #" + requestId
                        ));

        String status = req.getStatus();
        // Chỉ cho confirm khi đang chờ thanh toán hoặc đã ở trạng thái paid
        if (!"ready_to_pay".equals(status) && !"paid".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể xác nhận thanh toán cho trạng thái hiện tại: " + status
            );
        }

        // Cập nhật trạng thái sang "paid"
        req.setStatus("paid");
        serviceRequests.save(req);

        // Payment = null, nhưng AdminPaymentRowDto.from(...) sẽ gán
        // paymentStatus = "PAID" vì status = "paid"
        return AdminPaymentRowDto.from(req, null);
    }
}
