package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.AuthCredential;
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.AuthCredentialRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final AuthCredentialRepository authCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          AuthCredentialRepository authCredentialRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authCredentialRepository = authCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String userHome() {
        return "user";
    }

    @GetMapping("/user/ai-quote")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String aiQuote() {
        return "user/ai-quote";
    }


    // Trang hồ sơ người dùng
    @GetMapping("/user/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String profile(Model model, Principal principal) {
        if (!model.containsAttribute("currentUser")) {
            String username = principal.getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElse(null);
            model.addAttribute("currentUser", currentUser);
        }
        return "user/profile";
    }

    @PostMapping("/user/profile/update-info")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String updateInfo(
            Principal principal,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phone,
            RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));


        if (!email.equals(currentUser.getEmail()) && userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email này đã được sử dụng!");
            redirectAttributes.addFlashAttribute("currentUser", currentUser);
            return "redirect:/user/profile";
        }


        String normalizedPhone = phone.replaceAll("\\s", "");

        if (!normalizedPhone.matches("^\\d{9,10}$")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại phải là 9 hoặc 10 chữ số!");
            redirectAttributes.addFlashAttribute("currentUser", currentUser);
            return "redirect:/user/profile";
        }

        if (firstName.trim().isEmpty() || lastName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Họ và tên không được để trống!");
            redirectAttributes.addFlashAttribute("currentUser", currentUser);
            return "redirect:/user/profile";
        }

        // --- 4. Lưu dữ liệu ---
        currentUser.setFirstName(firstName.trim());
        currentUser.setLastName(lastName.trim());
        currentUser.setEmail(email);
        currentUser.setPhone(normalizedPhone);
        userRepository.save(currentUser);

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        return "redirect:/user/profile";
    }

    @PostMapping("/user/profile/update-password")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String updatePassword(
            Principal principal,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Integer userId = currentUser.getUserId();

        AuthCredential auth = authCredentialRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đăng nhập"));

        if (!passwordEncoder.matches(currentPassword, auth.getPasswordHash())) {
            redirectAttributes.addFlashAttribute("errorMessagePass", "Mật khẩu hiện tại không đúng!");
            return "redirect:/user/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessagePass", "Mật khẩu nhập lại không khớp!");
            return "redirect:/user/profile";
        }

        if (newPassword.length() < 5) {
            redirectAttributes.addFlashAttribute("errorMessagePass", "Mật khẩu mới phải có ít nhất 6 ký tự!");
            return "redirect:/user/profile";
        }

        auth.setPasswordHash(passwordEncoder.encode(newPassword));

        authCredentialRepository.save(auth);

        redirectAttributes.addFlashAttribute("successMessagePass", "Đổi mật khẩu thành công!");
        return "redirect:/user/profile";
    }
}