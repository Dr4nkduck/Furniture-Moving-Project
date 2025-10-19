package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.service.ForgotPasswordService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.OffsetDateTime;

@Controller
@RequestMapping("/forgot")
public class ForgotPasswordController {

    private static final String FORGOT_SESSION_KEY = "FORGOT_VERIFIED"; // session gate cho bước 3

    private final ForgotPasswordService forgotPasswordService;

    public ForgotPasswordController(ForgotPasswordService forgotPasswordService) {
        this.forgotPasswordService = forgotPasswordService;
    }

    /* ==================== BƯỚC 1: NHẬP EMAIL ==================== */
    @GetMapping
    public String showEmailForm(Model model,
                                @RequestParam(value = "sent", required = false) String sent,
                                @RequestParam(value = "email", required = false) String email) {
        model.addAttribute("email", email == null ? "" : email);
        model.addAttribute("sent", sent != null);
        return "accountmanage/forgot-email";
    }

    @PostMapping("/request")
    public String requestOtp(@RequestParam("email") String email) {
        // Không lộ thông tin tồn tại email hay không
        forgotPasswordService.requestOtp(email);
        // ✅ CHỈNH: chuyển thẳng tới trang nhập OTP kèm email
        return "redirect:/forgot/verify?email=" + email;
    }

    /* ==================== BƯỚC 2: NHẬP OTP ==================== */
   // ForgotPasswordController.java

@GetMapping("/verify")
public String showVerifyForm(@RequestParam("email") String email, Model model) {
    email = normalizeEmail(email);
    model.addAttribute("email", email);
    return "accountmanage/forgot-verify";
}

@PostMapping("/verify")
public String verifyOtp(@RequestParam("email") String email,
                        @RequestParam("otp") String otp,
                        HttpSession session,
                        Model model) {
    email = normalizeEmail(email);                // ✅ chuẩn hoá trước khi dùng
    try {
        forgotPasswordService.verifyOtp(email, otp);
        session.setAttribute(FORGOT_SESSION_KEY, new Verified(email, otp, java.time.OffsetDateTime.now()));
        return "redirect:/forgot/reset";
    } catch (IllegalArgumentException ex) {
        model.addAttribute("email", email);
        model.addAttribute("error", ex.getMessage());
        return "accountmanage/forgot-verify";
    }
}

/** Nhận "a@b.com,a@b.com" -> "a@b.com" */
private String normalizeEmail(String email) {
    if (email == null) return null;
    email = email.trim();
    int comma = email.indexOf(',');
    if (comma >= 0) email = email.substring(0, comma);
    return email;
}


    /* ==================== BƯỚC 3: ĐẶT LẠI MẬT KHẨU ==================== */
    @GetMapping("/reset")
    public String showResetForm(HttpSession session) {
        Verified v = (Verified) session.getAttribute(FORGOT_SESSION_KEY);
        if (v == null) return "redirect:/forgot"; // chưa verify OTP
        return "accountmanage/forgot-reset";
    }

    @PostMapping("/reset")
public String resetPassword(@RequestParam("newPassword") String newPassword,
                            @RequestParam("confirmPassword") String confirmPassword,
                            HttpSession session,
                            Model model) {
    Verified v = (Verified) session.getAttribute(FORGOT_SESSION_KEY);
    if (v == null) return "redirect:/forgot"; // chưa verify OTP

    // ✅ Kiểm tra khớp 2 mật khẩu
    if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
        model.addAttribute("error", "Mật khẩu nhập lại không khớp. Vui lòng thử lại.");
        return "accountmanage/forgot-reset";
    }

    // (Tuỳ chọn) kiểm tra độ dài / chính sách mật khẩu
    if (newPassword.length() < 6) {
        model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
        return "accountmanage/forgot-reset";
    }

    try {
        forgotPasswordService.resetPassword(v.email, v.otp, newPassword);
        // ✅ xong thì xoá session gate
        session.removeAttribute(FORGOT_SESSION_KEY);
        return "redirect:/login?resetSuccess=true";
    } catch (IllegalArgumentException ex) {
        model.addAttribute("error", ex.getMessage());
        return "accountmanage/forgot-reset";
    }
}


    /* ==================== DTO session ==================== */
    public static class Verified {
        public final String email;
        public final String otp;
        public final OffsetDateTime verifiedAt;

        public Verified(String email, String otp, OffsetDateTime at) {
            this.email = email;
            this.otp = otp;
            this.verifiedAt = at;
        }
    }
}
