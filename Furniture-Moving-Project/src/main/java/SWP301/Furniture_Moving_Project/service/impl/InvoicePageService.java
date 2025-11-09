package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.repository.CustomerRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import SWP301.Furniture_Moving_Project.repository.projection.InvoicePageView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InvoicePageService {

    private final CustomerRepository customerRepo;
    private final ServiceRequestRepository srRepo;

    public InvoicePageService(CustomerRepository customerRepo, ServiceRequestRepository srRepo) {
        this.customerRepo = customerRepo;
        this.srRepo = srRepo;
    }

    public Integer customerIdFromUsername(String username) {
        return customerRepo.findCustomerIdByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy customer cho user"));
    }

    public InvoicePageView getInvoiceForCustomer(Integer customerId, Integer requestId) {
        return srRepo.findInvoiceForCustomer(customerId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn"));
    }

    public InvoicePageView getLatestInvoiceForCustomer(Integer customerId) {
        return srRepo.findLatestInvoiceForCustomer(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có đơn"));
    }
}
