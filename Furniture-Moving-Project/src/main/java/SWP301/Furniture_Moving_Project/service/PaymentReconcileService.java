package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.BankTransactionDTO;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PaymentReconcileService {

    private final ServiceRequestRepository serviceRequestRepository;

    private static final Pattern REQ_PATTERN = Pattern.compile("REQ(\\d+)", Pattern.CASE_INSENSITIVE);

    public PaymentReconcileService(ServiceRequestRepository serviceRequestRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @Transactional
    public void reconcile(List<BankTransactionDTO> txns) {
        for (BankTransactionDTO tx : txns) {
            try {
                Integer requestId = extractRequestId(tx.getDescription());
                if (requestId == null) {
                    // Không có REQxxx trong nội dung, bỏ qua
                    continue;
                }

                ServiceRequest sr = serviceRequestRepository.findById(requestId)
                        .orElse(null);
                if (sr == null) continue;

                // Nếu đã paid rồi thì bỏ qua
                if ("paid".equalsIgnoreCase(sr.getStatus())) {
                    continue;
                }

                BigDecimal total = sr.getTotalCost();
                if (total == null) continue;

                BigDecimal amount = tx.getAmount().setScale(0, RoundingMode.HALF_UP);
                BigDecimal full   = total.setScale(0, RoundingMode.HALF_UP);
                BigDecimal deposit20 = full.multiply(new BigDecimal("0.20")).setScale(0, RoundingMode.HALF_UP);

                // So sánh amount: nếu bằng 100% hoặc 20% thì coi là hợp lệ
                // (có thể cho phép lệch vài đồng nếu cần)
                if (amount.compareTo(full) == 0) {
                    sr.setStatus("paid");
                    // TODO: nếu sau này có thêm paymentType or paidAt thì set ở đây
                } else if (amount.compareTo(deposit20) == 0) {
                    sr.setStatus("deposit_paid");
                } else {
                    // Số tiền không khớp, bỏ qua (hoặc log lại)
                    continue;
                }

                serviceRequestRepository.save(sr);
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
