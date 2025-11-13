package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.ContractRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ContractService {

    private final ContractRepository contracts;
    private final UserRepository users;

    public ContractService(ContractRepository contracts, UserRepository users) {
        this.contracts = contracts;
        this.users = users;
    }

    public boolean hasAccepted(Integer userId) {
        if (userId == null) return false;
        return contracts.existsByUser_UserIdAndStatus(userId, "signed") 
            || contracts.existsByUser_UserIdAndStatus(userId, "acknowledged");
    }

    @Transactional
    public Contract accept(Integer userId) {
        User u = users.findById(userId).orElseThrow();
        Contract c = new Contract();
        c.setUser(u);
        c.setStatus("signed"); // Changed from "accepted" to "signed"
        c.setSignedAt(OffsetDateTime.now());
        // providerId will be set when request is created
        return contracts.save(c);
    }

    public Contract findByUserId(Integer userId) {
        List<Contract> allContracts = contracts.findByUser_UserId(userId);
        return allContracts.stream()
            .filter(c -> "signed".equals(c.getStatus()) || "acknowledged".equals(c.getStatus()))
            .findFirst()
            .orElse(null);
    }
}
