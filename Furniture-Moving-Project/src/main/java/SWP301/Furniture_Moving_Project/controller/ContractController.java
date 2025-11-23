package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.Contract;
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.ContractRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import SWP301.Furniture_Moving_Project.service.ContractService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Controller
@RequestMapping("/contract")
public class ContractController {

    private final ContractService contracts;
    private final UserRepository users;
    private final ContractRepository contractRepository;

    public ContractController(ContractService contracts, UserRepository users, ContractRepository contractRepository) {
        this.contracts = contracts;
        this.users = users;
        this.contractRepository = contractRepository;
    }

    @GetMapping
    public String showContract(Model model) {
        Integer userId = currentUserId();
        if (userId == null) return "redirect:/login";

        // Luôn cho vào trang hợp đồng, không redirect khi đã accepted
        addLoginInfo(model, userId);

        User u = users.findById(userId).orElse(null);
        model.addAttribute("customerName", buildName(u));
        return "contract/contract";
    }

    @PostMapping("/accept")
    public String acceptContract() {
        Integer userId = currentUserId();
        if (userId == null) return "redirect:/login";
        contracts.accept(userId);
        return "redirect:/request";
    }

    /**
     * View a specific contract by ID
     * GET /contract/{id}
     */
    @GetMapping("/{id}")
    public String viewContract(@PathVariable("id") Integer contractId, Model model) {
        Integer userId = currentUserId();
        if (userId == null) return "redirect:/login";

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found"));

        // Verify the contract belongs to the current user
        if (contract.getUser() == null || !contract.getUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to view this contract");
        }

        addLoginInfo(model, userId);
        model.addAttribute("contract", contract);
        
        User u = users.findById(userId).orElse(null);
        model.addAttribute("customerName", buildName(u));
        
        return "contract/contract";
    }

    // ===== helpers =====
    private Integer currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object p = auth.getPrincipal();

        if (p instanceof User) return ((User) p).getUserId();

        if (p instanceof org.springframework.security.core.userdetails.User su) {
            String username = su.getUsername();

            Optional<User> byUsername = users.findByUsernameIgnoreCase(username);
            if (byUsername.isPresent()) return byUsername.get().getUserId();

            Optional<User> byEmail = users.findByEmailIgnoreCase(username);
            if (byEmail.isPresent()) return byEmail.get().getUserId();

            return users.findByUsername(username)
                        .map(User::getUserId)
                        .orElse(null);
        }

        if (p instanceof String s && "anonymousUser".equals(s)) return null;

        return null;
    }

    private String buildName(User u) {
        if (u == null) return "Khách hàng";
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName()  != null ? u.getLastName().trim()  : "";
        String full = (fn + " " + ln).trim();
        if (!full.isEmpty()) return full;
        if (u.getUsername() != null && !u.getUsername().isEmpty()) return u.getUsername();
        if (u.getEmail() != null && !u.getEmail().isEmpty()) return u.getEmail();
        return "Khách hàng";
    }

    /** Thêm biến isLoggedIn/currentUser cho navbar */
    private void addLoginInfo(Model model, Integer userId) {
        if (userId != null) {
            users.findById(userId).ifPresent(u -> {
                model.addAttribute("isLoggedIn", true);
                model.addAttribute("currentUser", u);
            });
        } else {
            model.addAttribute("isLoggedIn", false);
        }
    }
}
