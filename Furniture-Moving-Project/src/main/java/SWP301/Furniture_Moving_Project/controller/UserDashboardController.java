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
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/dashboard") // <-- path riêng, KHÔNG dùng "/"
public class UserDashboardController {

    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public UserDashboardController(UserRepository userRepository,
                                   ServiceRequestRepository serviceRequestRepository) {
        this.userRepository = userRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @GetMapping
    public String userDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            model.addAttribute("user", null);
            model.addAttribute("orders", List.of());
            return "user/dashboard"; // templates/user/dashboard.html
        }

        String principal = auth.getName(); // username hoặc email
        User u = userRepository.findByEmail(principal)
                .orElseGet(() -> userRepository.findByUsername(principal).orElse(null));

        if (u == null) {
            model.addAttribute("user", null);
            model.addAttribute("orders", List.of());
            return "user/dashboard";
        }

        Integer customerId = u.getUserId(); // nếu bạn tách bảng Customer, map sang customerId tương ứng

        List<ServiceRequest> rows =
                serviceRequestRepository.findTop5ByCustomerIdOrderByCreatedAtDesc(customerId);

        List<OrderSummaryDTO> orders = rows.stream().map(sr ->
                new OrderSummaryDTO(
                        sr.getRequestId(),
                        "#" + sr.getRequestId(),
                        sr.getStatus(),
                        sr.getCreatedAt()
                )
        ).toList();

        model.addAttribute("user",
                new UserMiniDTO(u.getUserId(), u.getFirstName() + " " + u.getLastName(), u.getEmail()));
        model.addAttribute("orders", orders);
        return "user/dashboard";
    }
}
