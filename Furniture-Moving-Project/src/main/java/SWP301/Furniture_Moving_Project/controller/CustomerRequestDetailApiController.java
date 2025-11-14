package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.ContractRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer/request")
public class CustomerRequestDetailApiController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ContractRepository contractRepository;
    private final JdbcTemplate jdbc;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CustomerRequestDetailApiController(
            ServiceRequestRepository serviceRequestRepository,
            ContractRepository contractRepository,
            JdbcTemplate jdbc
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.contractRepository = contractRepository;
        this.jdbc = jdbc;
    }

    private Integer getCurrentCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String username = auth.getName();

        try {
            return jdbc.queryForObject("""
                SELECT c.customer_id
                FROM customers c
                JOIN users u ON u.user_id = c.user_id
                WHERE u.username = ?
            """, Integer.class, username);
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/{requestId}")
    public DetailDTO getRequestDetail(@PathVariable Integer requestId) {
        Integer customerId = getCurrentCustomerId();

        Optional<ServiceRequest> reqOpt = serviceRequestRepository.findById(requestId);
        if (reqOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        ServiceRequest r = reqOpt.get();
        if (!r.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Contract c = null;
        if (r.getContractId() != null) {
            c = contractRepository.findById(r.getContractId()).orElse(null);
        }

        return DetailDTO.from(r, c, dtf);
    }

    // DTO
    public static class DetailDTO {
        public Integer requestId;
        public String status;
        public String paymentStatus;
        public String paymentType;
        public String totalCostFormatted;
        public String depositFormatted;
        public String paidAtFormatted;

        public Integer contractId;
        public String contractStatus;
        public String contractSignedAtFormatted;
        public String contractAckAtFormatted;

        public static DetailDTO from(ServiceRequest r, Contract c, DateTimeFormatter dtf) {
            DetailDTO dto = new DetailDTO();
            dto.requestId = r.getRequestId();
            dto.status = r.getStatus();
            dto.paymentStatus = r.getPaymentStatus();
            dto.paymentType = r.getPaymentType();

            if (r.getTotalCost() != null)
                dto.totalCostFormatted = String.format("%,.0f đ", r.getTotalCost());

            if (r.getDepositAmount() != null)
                dto.depositFormatted = String.format("%,.0f đ", r.getDepositAmount());

            if (r.getPaidAt() != null)
                dto.paidAtFormatted = dtf.format(r.getPaidAt());

            if (c != null) {
                dto.contractId = c.getContractId();
                dto.contractStatus = c.getStatus();

                if (c.getSignedAt() != null)
                    dto.contractSignedAtFormatted = dtf.format(c.getSignedAt());

                if (c.getAcknowledgedAt() != null)
                    dto.contractAckAtFormatted = dtf.format(c.getAcknowledgedAt());
            }

            return dto;
        }
    }
}
