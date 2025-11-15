package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.repository.ContractRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/customer/requests")
public class CustomerRequestContractViewController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ContractRepository contractRepository;

    public CustomerRequestContractViewController(ServiceRequestRepository serviceRequestRepository,
                                                 ContractRepository contractRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.contractRepository = contractRepository;
    }

    /**
     * Xem hợp đồng của 1 request (chỉ xem, không ký)
     * URL: /customer/requests/{id}/contract
     */
    @GetMapping("/{id}/contract")
    public String viewContract(@PathVariable("id") Integer requestId,
                               Authentication authentication,
                               Model model) {

        // Bắt buộc đăng nhập (giống các trang customer khác)
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // 1) Lấy ServiceRequest
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu"));

        // 2) Kiểm tra xem yêu cầu này có gắn contract không
        Integer contractId = request.getContractId();
        if (contractId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Yêu cầu này chưa có hợp đồng đính kèm");
        }

        // 3) Lấy Contract theo contractId
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Không tìm thấy hợp đồng #" + contractId));

        // 4) Đẩy dữ liệu ra view
        model.addAttribute("request", request);
        model.addAttribute("contract", contract);

        // Nếu sau này bạn muốn inject tên khách:
        // model.addAttribute("customerName", ...);

        return "customer/request-contract-view"; // file .html view-only
    }
}
