package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.BankTransactionDTO;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PaymentReconcileService {

    private final ServiceRequestService serviceRequestService;

    private static final Pattern REQ_PATTERN = Pattern.compile("REQ(\\d+)", Pattern.CASE_INSENSITIVE);

    public PaymentReconcileService(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    /**
     * Đối soát sao kê ngân hàng:
     *  - Tìm REQ{id} trong description
     *  - So sánh amount với:
     *      + 100% total_cost => FULL
     *      + 20% total_cost  => DEPOSIT_20
     *  - Gọi ServiceRequestService.markAsPaid(...) để cập nhật
     *
     *  Không còn sửa ServiceRequest.status = 'paid' / 'deposit_paid' nữa.
     */
    @Transactional
    public void reconcile(List<BankTransactionDTO> txns) {
        if (txns == null || txns.isEmpty()) return;

        for (BankTransactionDTO tx : txns) {
            try {
                Integer requestId = extractRequestId(tx.getDescription());
                if (requestId == null) {
                    // Không có REQxxx trong nội dung, bỏ qua
                    continue;
                }

                ServiceRequest sr = serviceRequestService.findById(requestId).orElse(null);
                if (sr == null) continue;

                // Nếu đã đánh dấu thanh toán rồi thì bỏ qua
                if ("PAID".equalsIgnoreCase(sr.getPaymentStatus())
                        || "DEPOSIT_PAID".equalsIgnoreCase(sr.getPaymentStatus())) {
                    continue;
                }

                BigDecimal total = sr.getTotalCost();
                if (total == null) continue;

                BigDecimal amount = tx.getAmount().setScale(0, RoundingMode.HALF_UP);
                BigDecimal full   = total.setScale(0, RoundingMode.HALF_UP);
                BigDecimal deposit20 = full.multiply(new BigDecimal("0.20")).setScale(0, RoundingMode.HALF_UP);

                String paymentType;
                if (amount.compareTo(full) == 0) {
                    paymentType = "FULL";
                } else if (amount.compareTo(deposit20) == 0) {
                    paymentType = "DEPOSIT_20";
                } else {
                    // Số tiền không khớp, bỏ qua (hoặc log lại)
                    continue;
                }

                // Ghi nhận thanh toán qua service (cập nhật payment_status, paid_at, deposit_amount, ...)
                serviceRequestService.markAsPaid(requestId, amount, paymentType);

            } catch (Exception ignored) {
                // Có thể log ra nếu cần
            }
        }
    }

    private Integer extractRequestId(String description) {
        if (description == null) return null;
        Matcher m = REQ_PATTERN.matcher(description);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
