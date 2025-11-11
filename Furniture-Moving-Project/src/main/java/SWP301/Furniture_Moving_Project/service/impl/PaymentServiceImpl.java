package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.config.PaymentProperties;
import SWP301.Furniture_Moving_Project.config.VietqrProperties;
import SWP301.Furniture_Moving_Project.dto.PaymentInitResponse;
import SWP301.Furniture_Moving_Project.dto.PaymentStatusResponse;
import SWP301.Furniture_Moving_Project.model.Payment;
import SWP301.Furniture_Moving_Project.model.PaymentStatus;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.PaymentRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
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

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    public PaymentServiceImpl(ServiceRequestRepository serviceRequestRepository,
                              PaymentRepository paymentRepository,
                              PaymentProperties paymentProps,
                              VietqrProperties vietqrProps) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.paymentRepository = paymentRepository;
        this.paymentProps = paymentProps;
        this.vietqrProps = vietqrProps;
    }

    @Override
    public PaymentInitResponse initPayment(Integer serviceRequestId) {
        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn vận chuyển"));

        BigDecimal amount = sr.getTotalCost();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Đơn chưa có giá hợp lệ để thanh toán");
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
    @Transactional(readOnly = true)
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

        return new PaymentStatusResponse(status, amount, paidAt);
    }
}
