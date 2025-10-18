// src/main/java/SWP301/Furniture_Moving_Project/controller/UserDashboardController.java
package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.OrderSummaryDTO;
import SWP301.Furniture_Moving_Project.dto.UserMiniDTO;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class UserDashboardController {

    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public UserDashboardController(UserRepository userRepository,
                                   ServiceRequestRepository serviceRequestRepository) {
        this.userRepository = userRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @GetMapping("/UserDashboard")
    public String userDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            model.addAttribute("user", null);
            model.addAttribute("orders", List.of());
            return "UserDashboard";
        }

        String principal = auth.getName(); // có thể là username hoặc email

        // Thử bằng email trước, không có thì bằng username
        User u = userRepository.findByEmail(principal)
                .orElseGet(() -> userRepository.findByUsername(principal).orElse(null));

        if (u == null) {
            model.addAttribute("user", null);
            model.addAttribute("orders", List.of());
            return "UserDashboard";
        }

        // NOTE: nếu hệ của bạn tách bảng customers (customer_id != users.user_id),
        // hãy thay dòng dưới bằng map user -> customerId qua CustomerRepository.
        Integer customerId = u.getUserId();

        // 5 đơn gần đây (cần method mới trong repo)
        List<ServiceRequest> rows =
                serviceRequestRepository.findTop5ByCustomerIdOrderByCreatedAtDesc(customerId);

        List<OrderSummaryDTO> orders = rows.stream().map(sr ->
                new OrderSummaryDTO(
                        sr.getRequestId(),
                        "#" + sr.getRequestId(),        // chưa có cột code -> phát sinh từ id
                        sr.getStatus(),
                        sr.getCreatedAt()               // entity của bạn dùng LocalDateTime
                )
        ).toList();

        model.addAttribute("user", new UserMiniDTO(u.getUserId(), u.getFirstName() + " " + u.getLastName(), u.getEmail()));
        model.addAttribute("orders", orders);
        return "UserDashboard";
    }

    @GetMapping("/")
    public String homeRedirect() { return "redirect:/UserDashboard"; }
}
