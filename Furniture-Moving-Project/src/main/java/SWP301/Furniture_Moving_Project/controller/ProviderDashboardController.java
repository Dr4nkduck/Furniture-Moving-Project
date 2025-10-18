package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.OrderDetailDTO;
import SWP301.Furniture_Moving_Project.dto.OrderSummaryDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderServiceItemDTO;
import SWP301.Furniture_Moving_Project.service.ProviderCatalogService;
import SWP301.Furniture_Moving_Project.service.ProviderOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/provider")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderDashboardController {

    private final ProviderCatalogService providerCatalogService;
    private final ProviderOrderService providerOrderService;

    public ProviderDashboardController(ProviderCatalogService providerCatalogService,
                                       ProviderOrderService providerOrderService) {
        this.providerCatalogService = providerCatalogService;
        this.providerOrderService = providerOrderService;
    }

    /* ---------- Pages ---------- */

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        Long providerId = resolveProviderId(principal);
        List<OrderSummaryDTO> assigned = providerOrderService.listOrders(providerId, List.of("ASSIGNED"));
        List<OrderSummaryDTO> inProgress = providerOrderService.listOrders(providerId, List.of("IN_PROGRESS"));
        List<OrderSummaryDTO> pendingOffers = providerOrderService.listOrders(providerId, List.of("PENDING_OFFER"));

        model.addAttribute("assignedCount", assigned.size());
        model.addAttribute("inProgressCount", inProgress.size());
        model.addAttribute("rfpCount", pendingOffers.size());
        return "provider/dashboard";
    }

    @GetMapping("/services")
    public String servicesPage() {
        return "provider/services";
    }

    @GetMapping("/orders")
    public String ordersPage() {
        return "provider/orders";
    }

    @GetMapping("/orders/{orderId}")
    public String orderDetailPage(@PathVariable Long orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "provider/order-detail";
    }

    /* ---------- APIs (JSON) ---------- */

    @GetMapping("/api/services")
    @ResponseBody
    public List<ProviderServiceItemDTO> listServices(Principal principal) {
        return providerCatalogService.listMyItems(resolveProviderId(principal));
    }

    @PostMapping("/api/services")
    @ResponseBody
    public ProviderServiceItemDTO createService(@RequestBody ProviderServiceItemDTO request, Principal principal) {
        return providerCatalogService.create(resolveProviderId(principal), request);
    }

    @PutMapping("/api/services/{serviceItemId}")
    @ResponseBody
    public ProviderServiceItemDTO updateService(@PathVariable Long serviceItemId,
                                                @RequestBody ProviderServiceItemDTO request,
                                                Principal principal) {
        return providerCatalogService.update(resolveProviderId(principal), serviceItemId, request);
    }

    @DeleteMapping("/api/services/{serviceItemId}")
    @ResponseBody
    public void deleteService(@PathVariable Long serviceItemId, Principal principal) {
        providerCatalogService.delete(resolveProviderId(principal), serviceItemId);
    }

    @GetMapping("/api/orders")
    @ResponseBody
    public List<OrderSummaryDTO> listOrders(@RequestParam(defaultValue = "PENDING_OFFER,ASSIGNED,IN_PROGRESS,COMPLETED,CANCELLED")
                                            String statuses,
                                            Principal principal) {
        List<String> statusList = Arrays.stream(statuses.split(",")).map(String::trim).toList();
        return providerOrderService.listOrders(resolveProviderId(principal), statusList);
    }

    @GetMapping("/api/orders/{orderId}")
    @ResponseBody
    public OrderDetailDTO getOrderDetail(@PathVariable Long orderId, Principal principal) {
        return providerOrderService.getOrderDetail(resolveProviderId(principal), orderId, true);
    }

    @PostMapping("/api/orders/{orderId}/accept")
    @ResponseBody
    public void acceptOrder(@PathVariable Long orderId,
                            @RequestParam(required = false) String note,
                            Principal principal) {
        providerOrderService.acceptRfp(resolveProviderId(principal), orderId, note);
    }

    @PostMapping("/api/orders/{orderId}/decline")
    @ResponseBody
    public void declineOrder(@PathVariable Long orderId,
                             @RequestParam(required = false) String reason,
                             Principal principal) {
        providerOrderService.declineRfp(resolveProviderId(principal), orderId, reason);
    }

    @PatchMapping("/api/orders/{orderId}/status")
    @ResponseBody
    public void updateOrderStatus(@PathVariable Long orderId,
                                  @RequestParam String to,
                                  @RequestParam(required = false) String eta,
                                  @RequestParam(required = false) String note,
                                  Principal principal) {
        providerOrderService.updateStatus(resolveProviderId(principal), orderId, to, eta, note);
    }

    /* ---------- Helpers ---------- */

    private Long resolveProviderId(Principal principal) {
        // TODO: resolve from logged-in user. Temporary: parse from principal.getName() if it’s numeric.
        return Long.parseLong(principal.getName());
    }
}
