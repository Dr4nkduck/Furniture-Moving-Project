package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.model.AuthCredential;
import SWP301.Furniture_Moving_Project.model.PasswordResetToken;
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.AuthCredentialRepository;
import SWP301.Furniture_Moving_Project.repository.PasswordResetTokenRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Service
public class ForgotPasswordService {

    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 10;

    private final UserRepository userRepo;
    private final AuthCredentialRepository authRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder encoder;
    private final EmailService emailService;
    private final ThinkingLogger tlog = ThinkingLogger.get(ForgotPasswordService.class);
    private final SecureRandom rnd = new SecureRandom();

    public ForgotPasswordService(UserRepository userRepo,
                                 AuthCredentialRepository authRepo,
                                 PasswordResetTokenRepository tokenRepo,
                                 PasswordEncoder encoder,
                                 EmailService emailService) {
        this.userRepo = userRepo;
        this.authRepo = authRepo;
        this.tokenRepo = tokenRepo;
        this.encoder = encoder;
        this.emailService = emailService;
    }

    @Transactional
    public void requestOtp(String email) {
        tlog.think("Yêu cầu OTP cho email={}", email);
        User user = userRepo.findByEmail(email).orElse(null);

        if (user == null) {
            tlog.step("Email không tồn tại -> trả OK để tránh lộ thông tin");
            return;
        }

        var now = OffsetDateTime.now();
        var activeOtp = tokenRepo.findActiveByUserId(user.getUserId(), now);
        String otp;

        if (activeOtp.isPresent()) {
            otp = activeOtp.get().getOtpCode();
            tlog.step("Reuse OTP còn hạn cho userId={}", user.getUserId());
        } else {
            tokenRepo.consumeAllForUser(user.getUserId());
            otp = String.format("%06d", rnd.nextInt(1_000_000));

            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getUserId());
            token.setOtpCode(otp);
            token.setExpiresAt(now.plusMinutes(OTP_TTL_MINUTES));
            tokenRepo.save(token);

            tlog.step("Tạo OTP mới cho userId={}, expiresAt={}", user.getUserId(), token.getExpiresAt());
        }

        emailService.sendOtp(email, otp);
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        tlog.think("Reset password cho email={} (OTP ẩn)", email);

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn"));

        AuthCredential cred = authRepo.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin xác thực"));

        var now = OffsetDateTime.now();

        // ✅ so sánh OffsetDateTime với OffsetDateTime
        if (cred.getAccountLockedUntil() != null && now.isBefore(cred.getAccountLockedUntil())) {
            tlog.warn("Tài khoản userId={} bị khoá đến {}", user.getUserId(), cred.getAccountLockedUntil());
            throw new IllegalArgumentException("Bạn đã thử quá số lần cho phép. Vui lòng thử lại sau ít phút.");
        }

        var token = tokenRepo.findActiveByUserId(user.getUserId(), now)
                .filter(t -> t.getOtpCode().equals(otp))
                .orElse(null);

        if (token == null) {
            int attempts = (cred.getFailedAttempts() == null ? 0 : cred.getFailedAttempts()) + 1;
            cred.setFailedAttempts(attempts);

            if (attempts >= MAX_OTP_ATTEMPTS) {
                cred.setAccountLockedUntil(now.plusMinutes(LOCK_MINUTES));
                cred.setFailedAttempts(0);
                tlog.warn("Sai OTP quá {} lần -> khoá userId={} {} phút", MAX_OTP_ATTEMPTS, user.getUserId(), LOCK_MINUTES);
            }
            authRepo.save(cred);
            throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
        }

        cred.setPasswordHash(encoder.encode(newPassword));
        cred.setFailedAttempts(0);
        cred.setAccountLockedUntil(null);
        authRepo.save(cred);

        token.setConsumed(true);
        tokenRepo.save(token);

        tlog.step("Đổi mật khẩu thành công cho userId={}", user.getUserId());

        
    }

    @Transactional(readOnly = true)
public void verifyOtp(String email, String otp) {
    tlog.think("Verify OTP cho email={} (OTP ẩn)", email);
    User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn"));
    AuthCredential cred = authRepo.findByUserId(user.getUserId())
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin xác thực"));

    var now = OffsetDateTime.now();
    if (cred.getAccountLockedUntil() != null && now.isBefore(cred.getAccountLockedUntil())) {
        tlog.warn("Tài khoản userId={} bị khoá đến {}", user.getUserId(), cred.getAccountLockedUntil());
        throw new IllegalArgumentException("Bạn đã thử quá số lần cho phép. Vui lòng thử lại sau ít phút.");
    }

    var token = tokenRepo.findActiveByUserId(user.getUserId(), now).orElse(null);
    if (token == null || !token.getOtpCode().equals(otp)) {
        int attempts = (cred.getFailedAttempts() == null ? 0 : cred.getFailedAttempts()) + 1;
        cred.setFailedAttempts(attempts);
        if (attempts >= MAX_OTP_ATTEMPTS) {
            cred.setAccountLockedUntil(now.plusMinutes(LOCK_MINUTES));
            cred.setFailedAttempts(0);
            tlog.warn("Sai OTP quá {} lần -> khoá userId={} {} phút", MAX_OTP_ATTEMPTS, user.getUserId(), LOCK_MINUTES);
        }
        authRepo.save(cred);
        throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
    }

    // ĐÚNG OTP: không consume, không đổi gì. Chỉ cho phép sang bước reset.
    tlog.step("OTP hợp lệ cho userId={}", user.getUserId());
}

}
