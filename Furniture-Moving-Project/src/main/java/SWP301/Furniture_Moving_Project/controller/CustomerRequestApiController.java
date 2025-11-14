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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer/requests")
public class CustomerRequestApiController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ContractRepository contractRepository;
    private final JdbcTemplate jdbc;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CustomerRequestApiController(ServiceRequestRepository serviceRequestRepository,
                                        ContractRepository contractRepository,
                                        JdbcTemplate jdbc) {
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
            return jdbc.queryForObject(
                    """
                    SELECT c.customer_id
                    FROM customers c
                    JOIN users u ON u.user_id = c.user_id
                    WHERE u.username = ?
                    """,
                    Integer.class,
                    username
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không tìm thấy hồ sơ khách hàng cho tài khoản hiện tại"
            );
        }
    }

    @GetMapping
    public List<MyRequestStatusDTO> getMyRequests(
            @RequestParam(value = "status", required = false) String status
    ) {
        Integer customerId = getCurrentCustomerId();

        List<ServiceRequest> requests;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            requests = serviceRequestRepository.findByCustomerIdAndStatus(customerId, status);
        } else {
            requests = serviceRequestRepository.findByCustomerId(customerId);
        }

        // lấy contract map
        Map<Integer, Contract> contractsMap = Collections.emptyMap();
        if (!requests.isEmpty()) {
            Set<Integer> cids = requests.stream()
                    .map(ServiceRequest::getContractId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!cids.isEmpty()) {
                List<Contract> contracts = contractRepository.findAllById(cids);
                contractsMap = contracts.stream()
                        .collect(Collectors.toMap(Contract::getContractId, Function.identity()));
            }
        }

        Map<Integer, Contract> finalContractsMap = contractsMap;

        return requests.stream()
                .map(r -> {
                    Contract c = null;
                    if (r.getContractId() != null) {
                        c = finalContractsMap.get(r.getContractId());
                    }
                    return MyRequestStatusDTO.from(r, c, dtf);
                })
                .collect(Collectors.toList());
    }

    public static class MyRequestStatusDTO {
        public Integer requestId;
        public String status;
        public String paymentStatus;
        public String paymentType;
        public String totalCostFormatted;
        public Integer contractId;
        public String contractStatus;
        public String contractSignedAtFormatted;

        public static MyRequestStatusDTO from(ServiceRequest r, Contract c, DateTimeFormatter dtf) {
            MyRequestStatusDTO dto = new MyRequestStatusDTO();
            dto.requestId = r.getRequestId();
            dto.status = r.getStatus();
            dto.paymentStatus = r.getPaymentStatus();
            dto.paymentType = r.getPaymentType();
            dto.totalCostFormatted = (r.getTotalCost() != null)
                    ? String.format("%,.0f đ", r.getTotalCost())
                    : "—";
            dto.contractId = r.getContractId();
            if (c != null) {
                dto.contractStatus = c.getStatus();
                if (c.getSignedAt() != null) {
                    dto.contractSignedAtFormatted = dtf.format(c.getSignedAt());
                }
            }
            return dto;
        }
    }
}
