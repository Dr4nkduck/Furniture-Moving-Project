package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.repository.projection.InvoicePageView;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Controller
public class ApprovalPageController {

    private final ServiceRequestRepository srRepo;
    private final ProviderRepository providerRepo;

    public ApprovalPageController(ServiceRequestRepository srRepo,
                                  ProviderRepository providerRepo) {
        this.srRepo = srRepo;
        this.providerRepo = providerRepo;
    }

    /** Trang duyệt hoá đơn: /approval?id=1
     *  - ADMIN xem được mọi đơn
     *  - PROVIDER chỉ xem được đơn của chính provider_id của mình
     */
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @GetMapping("/approval")
    public String approval(@RequestParam("id") Integer requestId,
                           Authentication auth,
                           Model model) {

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        Optional<InvoicePageView> opt;
        if (isAdmin) {
            opt = srRepo.findInvoiceById(requestId);
        } else {
            Integer providerId = providerRepo.findProviderIdByUsername(auth.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
            opt = srRepo.findInvoiceByIdForProvider(requestId, providerId);
        }

        InvoicePageView v = opt.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hoá đơn/request"));

        // Bind model cho Thymeleaf
        model.addAttribute("requestId", v.getInvoiceId());
        model.addAttribute("status", v.getStatus());
        model.addAttribute("invoiceNo", v.getInvoiceId());
        model.addAttribute("invoiceDate", v.getInvoiceDate());
        model.addAttribute("providerCompanyName", v.getProviderCompany());
        model.addAttribute("pickupAddress", v.getPickupAddress());
        model.addAttribute("deliveryAddress", v.getDeliveryAddress());
        model.addAttribute("furnitureItemsJson", v.getFurnitureItemsJson());
        model.addAttribute("totalCost", v.getTotalCost());

        return "approval/approval";
    }
}

