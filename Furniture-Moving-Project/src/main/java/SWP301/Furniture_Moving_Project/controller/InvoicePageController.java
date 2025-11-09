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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Controller
public class InvoicePageController {

    private final ServiceRequestRepository repo;
    private final ProviderRepository providerRepo;

    public InvoicePageController(ServiceRequestRepository repo, ProviderRepository providerRepo) {
        this.repo = repo;
        this.providerRepo = providerRepo;
    }

    // Trang phê duyệt cho Provider/Admin: /provider/invoices/{id}/approval
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @GetMapping("/provider/invoices/{id}/approval")
    public String showApprovalPage(@PathVariable("id") Integer id, Authentication auth, Model model) {

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        Optional<InvoicePageView> opt;
        if (isAdmin) {
            opt = repo.findInvoiceById(id);
        } else {
            Integer providerId = providerRepo.findProviderIdByUsername(auth.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không phải PROVIDER"));
            opt = repo.findInvoiceByIdForProvider(id, providerId);
        }

        InvoicePageView v = opt.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hoá đơn/request"));

        // Map dữ liệu sang biến Thymeleaf
        model.addAttribute("requestId",        v.getInvoiceId()); // data-request-id cho JS
        model.addAttribute("status",           v.getStatus());
        model.addAttribute("invoiceNo",        v.getInvoiceId());
        model.addAttribute("invoiceDate",      v.getInvoiceDate());
        model.addAttribute("providerCompanyName", v.getProviderCompany());
        model.addAttribute("pickupAddress",    v.getPickupAddress());
        model.addAttribute("deliveryAddress",  v.getDeliveryAddress());
        model.addAttribute("furnitureItemsJson", v.getFurnitureItemsJson());

        return "provider/invoices/approval";
    }
}
