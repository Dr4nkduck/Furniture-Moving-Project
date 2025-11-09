package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.CustomerRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.repository.projection.InvoicePageView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/approveApplication")
public class ApproveApplicationController {

    private final CustomerRepository customerRepo;
    private final ServiceRequestRepository srRepo;

    public ApproveApplicationController(CustomerRepository customerRepo,
                                        ServiceRequestRepository srRepo) {
        this.customerRepo = customerRepo;
        this.srRepo = srRepo;
    }

    // GET /approveApplication?id=...
    @GetMapping
    public String openPage(@RequestParam(value = "id", required = false) Integer requestId,
                           Authentication auth,
                           Model model) {

        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        String username = auth.getName();
        Optional<Integer> optCustomerId = customerRepo.findCustomerIdByUsername(username);
        if (optCustomerId.isEmpty()) {
            fillEmptyModel(model);
            return "approveApplication/approveApplication";
        }
        Integer customerId = optCustomerId.get();

        Optional<InvoicePageView> optView = (requestId != null)
                ? srRepo.findInvoiceForCustomer(customerId, requestId)
                : srRepo.findLatestInvoiceForCustomer(customerId);

        if (optView.isEmpty()) {
            fillEmptyModel(model);
            return "approveApplication/approveApplication";
        }

        bindModel(model, optView.get());
        return "approveApplication/approveApplication";
    }

    // GET /approveApplication/{id}
    @GetMapping("/{id}")
    public String openPageByPath(@PathVariable("id") Integer requestId,
                                 Authentication auth,
                                 Model model) {

        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        String username = auth.getName();
        Optional<Integer> optCustomerId = customerRepo.findCustomerIdByUsername(username);
        if (optCustomerId.isEmpty()) {
            fillEmptyModel(model);
            return "approveApplication/approveApplication";
        }
        Integer customerId = optCustomerId.get();

        Optional<InvoicePageView> optView = srRepo.findInvoiceForCustomer(customerId, requestId);
        if (optView.isEmpty()) {
            fillEmptyModel(model);
            return "approveApplication/approveApplication";
        }

        bindModel(model, optView.get());
        return "approveApplication/approveApplication";
    }

    /** Đổ dữ liệu vào model cho view */
    private void bindModel(Model model, InvoicePageView v) {
        model.addAttribute("status", v.getStatus());
        model.addAttribute("providerCompanyName", v.getProviderCompany());
        model.addAttribute("pickupAddress", v.getPickupAddress());
        model.addAttribute("deliveryAddress", v.getDeliveryAddress());
        model.addAttribute("invoiceNo", String.valueOf(v.getInvoiceId()));
        model.addAttribute("invoiceDate", v.getInvoiceDate());
        model.addAttribute("totalCost", v.getTotalCost());
        model.addAttribute("furnitureItemsJson", v.getFurnitureItemsJson());
    }

    /** Model rỗng an toàn */
    private void fillEmptyModel(Model model) {
        model.addAttribute("status", "pending");
        model.addAttribute("providerCompanyName", null);
        model.addAttribute("pickupAddress", "---");
        model.addAttribute("deliveryAddress", "---");
        model.addAttribute("invoiceNo", "---");
        model.addAttribute("invoiceDate", "---");
        model.addAttribute("totalCost", "0");
        model.addAttribute("furnitureItemsJson", "[]");
    }
}
