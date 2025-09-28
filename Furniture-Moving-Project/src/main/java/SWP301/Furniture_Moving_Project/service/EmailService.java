package SWP301.Furniture_Moving_Project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Gửi email OTP, có log THINK/STEP và bắt lỗi + from rõ ràng */
@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final ThinkingLogger tlog = ThinkingLogger.get(EmailService.class);

    @Value("${app.mail.from:${spring.mail.username}}")
    private String from; // mặc định lấy từ spring.mail.username nếu app.mail.from không set

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp) {
        tlog.think("Chuẩn bị gửi OTP tới {}", to);
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);                    // ✅ quan trọng
            msg.setTo(to);
            msg.setSubject("OTP khôi phục mật khẩu - Furniture Moving");
            msg.setText("""
                    Xin chào,

                    Mã OTP của bạn là: %s
                    Mã có hiệu lực trong 5 phút. Vui lòng KHÔNG chia sẻ mã này cho bất kỳ ai.

                    Trân trọng,
                    Furniture Moving Team
                    """.formatted(otp));

            mailSender.send(msg);
            tlog.step("Đã gửi OTP thành công tới {}", to);
        } catch (Exception ex) {
            tlog.err("Gửi mail thất bại: {}", ex.getMessage());
            // Có thể ném tiếp nếu muốn hiển thị lỗi lên UI:
            // throw new IllegalStateException("Không gửi được email OTP. Vui lòng thử lại sau.");
        }
    }
}
