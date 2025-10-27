package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class ServiceController {
    private final UserRepository userRepository;

    public ServiceController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Helper method để thêm thông tin user vào model
    private void addUserInfoToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Optional<User> userOpt = userRepository.findByUsername(auth.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("currentUser", user);
                model.addAttribute("isLoggedIn", true);
            }
        } else {
            model.addAttribute("isLoggedIn", false);
        }
    }

    // 1. Chuyển nhà trọn gói
    @GetMapping("/service/chuyen-nha")
    public String chuyenNha(Model model) {
        addUserInfoToModel(model);
        model.addAttribute("serviceName", "Chuyển nhà trọn gói");
        model.addAttribute("serviceDescription", "Dịch vụ chuyển nhà trọn gói với đầy đủ tính năng từ A-Z");
        return "service/chuyen-nha";
    }

    // 2. Chuyển văn phòng
    @GetMapping("/service/chuyen-van-phong")
    public String chuyenVanPhong(Model model) {
        addUserInfoToModel(model);
        model.addAttribute("serviceName", "Chuyển văn phòng");
        model.addAttribute("serviceDescription", "Dịch vụ chuyển văn phòng chuyên nghiệp, nhanh chóng");
        return "service/chuyen-van-phong";
    }

    // 3. Bốc xếp & kho bãi
    @GetMapping("/service/boc-xep-kho-bai")
    public String bocXepKhoBai(Model model) {
        addUserInfoToModel(model);
        model.addAttribute("serviceName", "Bốc xếp & kho bãi");
        model.addAttribute("serviceDescription", "Dịch vụ bốc xếp và quản lý kho bãi chuyên nghiệp");
        return "service/boc-xep-kho-bai";
    }

    // 4. Tháo lắp & đóng gói
    @GetMapping("/service/thao-lap-dong-goi")
    public String thaoLapDongGoi(Model model) {
        addUserInfoToModel(model);
        model.addAttribute("serviceName", "Tháo lắp & đóng gói");
        model.addAttribute("serviceDescription", "Dịch vụ tháo lắp và đóng gói đồ đạc an toàn");
        return "service/thao-lap-dong-goi";
    }

    // 5. Vận chuyển liên tỉnh
    @GetMapping("/service/van-chuyen-lien-tinh")
    public String vanChuyenLienTinh(Model model) {
        addUserInfoToModel(model);
        model.addAttribute("serviceName", "Vận chuyển liên tỉnh");
        model.addAttribute("serviceDescription", "Dịch vụ vận chuyển hàng hóa liên tỉnh an toàn, nhanh chóng");
        return "service/van-chuyen-lien-tinh";
    }

    // 6. Cho thuê xe tải
    @GetMapping("/service/cho-thue-xe-tai")
    public String choThueXeTai(Model model) {
        addUserInfoToModel(model);
        model.addAttribute("serviceName", "Cho thuê xe tải");
        model.addAttribute("serviceDescription", "Dịch vụ cho thuê xe tải đa dạng, phù hợp mọi nhu cầu");
        return "service/cho-thue-xe-tai";
    }
}
