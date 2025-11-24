package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.PaymentInitResponse;
import SWP301.Furniture_Moving_Project.dto.PaymentStatusResponse;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ServiceRequestRepository serviceRequestRepository;

    // Cấu hình VietQR từ application.properties
    @Value("${vietqr.bankCode}")
    private String bankCode;

    @Value("${vietqr.accountNumber}")
    private String accountNumber;

    @Value("${vietqr.accountName}")
    private String accountName;

    @Value("${payment.expireMinutes:15}")
    private long expireMinutes;

    @Value("${payment.addInfoPrefix:REQ}")
    private String addInfoPrefix;

    public PaymentServiceImpl(ServiceRequestRepository serviceRequestRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
    }

    // ====== Interface ======
    @Override
    public PaymentInitResponse initPayment(Integer serviceRequestId) {
        // Mặc định FULL nếu controller không truyền paymentType
        return initPayment(serviceRequestId, "FULL");
    }

    /**
     * Overload dùng cho controller:
     * - FULL: thanh toán toàn bộ
     * - DEPOSIT / DEPOSIT_20: đặt cọc 20%
     */
    public PaymentInitResponse initPayment(Integer serviceRequestId, String paymentType) {
        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ServiceRequest #" + serviceRequestId));

        if (sr.getTotalCost() == null || sr.getTotalCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Đơn chưa có tổng giá hợp lệ để thanh toán");
        }

        // Chuẩn hoá kiểu thanh toán
        String normalizedType;
        if ("DEPOSIT_20".equalsIgnoreCase(paymentType) || "DEPOSIT".equalsIgnoreCase(paymentType)) {
            normalizedType = "DEPOSIT_20"; // chuẩn hoá: dùng DEPOSIT_20
        } else {
            normalizedType = "FULL";
        }

        BigDecimal total = sr.getTotalCost();
        BigDecimal amount;

        if ("DEPOSIT_20".equals(normalizedType)) {
            // Đặt cọc 20%
            amount = total.multiply(new BigDecimal("0.20"));
        } else {
            // Thanh toán toàn bộ
            amount = total;
        }

        // Làm tròn sang VND integer
        amount = amount.setScale(0, RoundingMode.HALF_UP);

        // === CẬP NHẬT THÔNG TIN THANH TOÁN VÀO ServiceRequest ===
        sr.setPaymentType(normalizedType);            // "DEPOSIT_20" hoặc "FULL"
        if ("DEPOSIT_20".equals(normalizedType)) {
            sr.setDepositAmount(amount);             // số tiền cọc 20%
        } else {
            sr.setDepositAmount(null);               // không lưu cọc nếu full
        }

        // Nếu chưa từng thanh toán thành công thì set trạng thái thanh toán = PENDING (hoặc UNPAID tuỳ bạn)
        // Admin sẽ là người đổi sang PAID / DEPOSIT_PAID
        if (sr.getPaymentStatus() == null ||
                (!"PAID".equalsIgnoreCase(sr.getPaymentStatus())
                        && !"DEPOSIT_PAID".equalsIgnoreCase(sr.getPaymentStatus()))) {
            sr.setPaymentStatus("PENDING");
        }

        serviceRequestRepository.save(sr);

        // === TÍNH HẾT HẠN PHIÊN ===
        OffsetDateTime expireAt = OffsetDateTime.now(ZONE_VN).plusMinutes(expireMinutes);

        // Nội dung chuyển khoản: REQ{requestId}
        String addInfo = addInfoPrefix + serviceRequestId;

        // Tạo URL ảnh QR (VietQR public)
        String base = "https://img.vietqr.io/image/" + bankCode + "-" + accountNumber + "-compact2.png";
        String query = String.format(
                "?amount=%d&addInfo=%s&accountName=%s",
                amount.longValue(),
                URLEncoder.encode(addInfo, StandardCharsets.UTF_8),
                URLEncoder.encode(accountName, StandardCharsets.UTF_8)
        );

        String vietqrImageUrl = base + query;

        PaymentInitResponse resp = new PaymentInitResponse();
        resp.setMode("VIETQR");
        resp.setAmount(amount);
        resp.setExpireAt(expireAt);
        resp.setVietqrImageUrl(vietqrImageUrl);

        // Không dùng payUrl / txnRef nữa
        resp.setPayUrl(null);
        resp.setTxnRef(null);

        return resp;
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(Integer serviceRequestId) {
        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ServiceRequest #" + serviceRequestId));

        // 1) Xác định trạng thái thanh toán hiển thị cho frontend
        String paymentStatusRaw = sr.getPaymentStatus();
        String workflow = sr.getStatus() == null ? "" : sr.getStatus().toLowerCase();

        String paymentStatus;
        if ("PAID".equalsIgnoreCase(paymentStatusRaw) || "DEPOSIT_PAID".equalsIgnoreCase(paymentStatusRaw)) {
            // Admin đã xác nhận đã nhận tiền (toàn bộ hoặc tiền cọc)
            paymentStatus = "PAID";
        } else if ("ready_to_pay".equalsIgnoreCase(workflow)) {
            // Provider đã ghi nhận hợp đồng, đơn đang ở bước chờ khách thanh toán
            paymentStatus = "READY_TO_PAY";
        } else {
            // Trạng thái ban đầu / chưa tới bước ready_to_pay
            paymentStatus = "UNPAID";
        }

        // 2) Số tiền hiển thị: nếu DEPOSIT_20 thì dùng depositAmount, ngược lại dùng totalCost
        BigDecimal amountForResponse;
        if ("DEPOSIT_20".equalsIgnoreCase(sr.getPaymentType()) && sr.getDepositAmount() != null) {
            amountForResponse = sr.getDepositAmount();
        } else {
            amountForResponse = sr.getTotalCost();
        }

        PaymentStatusResponse resp = new PaymentStatusResponse();
        resp.setStatus(paymentStatus);
        resp.setAmount(amountForResponse);
        resp.setPaidAt(sr.getPaidAt());             // dùng field paidAt của entity (admin set)
        resp.setPaymentType(sr.getPaymentType());   // "DEPOSIT_20" hoặc "FULL"

        return resp;
    }
}
