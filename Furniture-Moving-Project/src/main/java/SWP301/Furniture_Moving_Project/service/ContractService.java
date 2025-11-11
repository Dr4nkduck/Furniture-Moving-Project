package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.ContractRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

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
        return contracts.existsByUser_UserIdAndStatus(userId, "accepted");
    }

    @Transactional
    public Contract accept(Integer userId) {
        User u = users.findById(userId).orElseThrow();
        Contract c = new Contract();
        c.setUser(u);
        c.setStatus("accepted");
        c.setSignedAt(OffsetDateTime.now());
        return contracts.save(c);
    }
}
