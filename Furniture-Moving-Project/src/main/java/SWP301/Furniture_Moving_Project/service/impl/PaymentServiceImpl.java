package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.config.PaymentProperties;
import SWP301.Furniture_Moving_Project.config.VietqrProperties;
import SWP301.Furniture_Moving_Project.dto.PaymentInitResponse;
import SWP301.Furniture_Moving_Project.dto.PaymentStatusResponse;
import SWP301.Furniture_Moving_Project.model.Payment;
import SWP301.Furniture_Moving_Project.model.PaymentStatus;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.PaymentRepository;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.service.EmailService;
import SWP301.Furniture_Moving_Project.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProperties paymentProps;
    private final VietqrProperties vietqrProps;
    private final ProviderRepository providerRepository;
    private final EmailService emailService;

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    public PaymentServiceImpl(ServiceRequestRepository serviceRequestRepository,
                              PaymentRepository paymentRepository,
                              PaymentProperties paymentProps,
                              VietqrProperties vietqrProps,
                              ProviderRepository providerRepository,
                              EmailService emailService) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.paymentRepository = paymentRepository;
        this.paymentProps = paymentProps;
        this.vietqrProps = vietqrProps;
        this.providerRepository = providerRepository;
        this.emailService = emailService;
    }

    @Override
    public PaymentInitResponse initPayment(Integer serviceRequestId) {
        return initPayment(serviceRequestId, "FULL");
    }

    public PaymentInitResponse initPayment(Integer serviceRequestId, String paymentType) {
        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn vận chuyển"));

        // Check if request is ready for payment
        if (!"ready_to_pay".equals(sr.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Đơn hàng chưa sẵn sàng để thanh toán. Vui lòng chờ nhà cung cấp xác nhận.");
        }

        BigDecimal totalCost = sr.getTotalCost();
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Đơn chưa có giá hợp lệ để thanh toán");
        }

        // Calculate amount based on payment type
        BigDecimal amount;
        if ("DEPOSIT".equalsIgnoreCase(paymentType)) {
            amount = totalCost.multiply(new BigDecimal("0.20")); // 20% deposit
        } else {
            amount = totalCost; // 100% full payment
        }

        ZonedDateTime now = ZonedDateTime.now(ZONE_VN);
        int expireMinutes = Math.max(1, paymentProps.getExpireMinutes());
        ZonedDateTime expireAt = now.plusMinutes(expireMinutes);

        String addInfoPrefix = (paymentProps.getAddInfoPrefix() == null || paymentProps.getAddInfoPrefix().isBlank())
                ? "REQ" : paymentProps.getAddInfoPrefix().trim();
        String addInfo = addInfoPrefix + serviceRequestId;

        Payment p = new Payment();
        p.setServiceRequestId(serviceRequestId);
        p.setAmount(amount);
        p.setPaymentType(paymentType != null ? paymentType.toUpperCase() : "FULL");
        p.setStatus(PaymentStatus.PENDING);
        p.setCreatedAt(now.toLocalDateTime());
        p.setExpireAt(expireAt.toLocalDateTime());
        p.setBankCode(vietqrProps.getBankCode());
        p.setAccountNumber(vietqrProps.getAccountNumber());
        p.setAccountName(vietqrProps.getAccountName());
        p.setAddInfo(addInfo);
        paymentRepository.save(p);

        long vnd = amount.setScale(0, RoundingMode.DOWN).longValue();
        String imgUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-qr_only.png?amount=%d&addInfo=%s&accountName=%s",
                enc(vietqrProps.getBankCode()),
                enc(vietqrProps.getAccountNumber()),
                vnd,
                enc(addInfo),
                enc(vietqrProps.getAccountName())
        );

        PaymentInitResponse resp = new PaymentInitResponse();
        resp.setVietqrImageUrl(imgUrl);
        resp.setBankCode(vietqrProps.getBankCode());
        resp.setAccountNumber(vietqrProps.getAccountNumber());
        resp.setAccountName(vietqrProps.getAccountName());
        resp.setAddInfo(addInfo);
        resp.setAmount(amount);
        resp.setExpireAt(expireAt.toOffsetDateTime());
        return resp;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public PaymentStatusResponse getPaymentStatus(Integer serviceRequestId) {
        Payment payment = paymentRepository
                .findTopByServiceRequestIdOrderByCreatedAtDesc(serviceRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch thanh toán gần nhất"));

        if (!payment.getServiceRequestId().equals(serviceRequestId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Giao dịch không thuộc về đơn này");
        }

        String status = payment.getStatus().name();
        BigDecimal amount = payment.getAmount();
        OffsetDateTime paidAt = payment.getPaidAt() == null
                ? null
                : payment.getPaidAt().atOffset(OffsetDateTime.now().getOffset());

        // Notify provider when payment is confirmed (PAID status)
        if ("PAID".equals(status) && paidAt != null) {
            notifyProviderIfNeeded(serviceRequestId, payment);
        }

        return new PaymentStatusResponse(status, amount, paidAt);
    }

    private void notifyProviderIfNeeded(Integer serviceRequestId, Payment payment) {
        try {
            ServiceRequest request = serviceRequestRepository.findById(serviceRequestId).orElse(null);
            if (request == null || request.getProviderId() == null) return;

            Provider provider = providerRepository.findById(request.getProviderId()).orElse(null);
            if (provider == null || provider.getUser() == null) return;

            User providerUser = provider.getUser();
            if (providerUser.getEmail() == null || providerUser.getEmail().isBlank()) return;

            // Format amount
            String amountStr = payment.getAmount().setScale(0, RoundingMode.DOWN).toPlainString();
            amountStr = amountStr.replaceAll("(\\d)(?=(\\d{3})+(?!\\d))", "$1,");

            emailService.notifyProviderPaymentSuccess(
                providerUser.getEmail(),
                serviceRequestId,
                payment.getPaymentType() != null ? payment.getPaymentType() : "FULL",
                amountStr
            );
        } catch (Exception e) {
            // Log error but don't fail the request
            System.err.println("Failed to notify provider: " + e.getMessage());
        }
    }
}
