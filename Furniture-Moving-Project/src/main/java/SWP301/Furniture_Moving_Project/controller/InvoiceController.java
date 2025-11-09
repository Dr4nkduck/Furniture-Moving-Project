package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.projection.InvoicePageView;
import SWP301.Furniture_Moving_Project.service.impl.InvoicePageService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/invoice")
public class InvoiceController {

    private final InvoicePageService service;

    public InvoiceController(InvoicePageService service) {
        this.service = service;
    }

    // /invoice/{id} : xem 1 đơn của user hiện tại
    @GetMapping("/{id}")
    public String view(@PathVariable Integer id, Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        var customerId = service.customerIdFromUsername(auth.getName());
        InvoicePageView v = service.getInvoiceForCustomer(customerId, id);
        bindModel(model, v);
        return "approveApplication/approveApplication";
    }

    // /invoice/latest : xem đơn mới nhất của user hiện tại
    @GetMapping("/latest")
    public String viewLatest(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        var customerId = service.customerIdFromUsername(auth.getName());
        var v = service.getLatestInvoiceForCustomer(customerId);
        bindModel(model, v);
        return "approveApplication/approveApplication";
    }

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
}
