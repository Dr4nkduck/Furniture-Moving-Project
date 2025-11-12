package SWP301.Furniture_Moving_Project.controller;


import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ProviderController {
    private final ProviderRepository providerRepository;

    public ProviderController(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @GetMapping("/provider/dashboard")
    public String dashboard() {
        return "provider/dashboard";
    }


    @GetMapping("/provider/services")
    public String services(Model model, Authentication auth) {
        String username = auth.getName();
        Provider p = providerRepository.findByUser_Username(username)
                .orElseThrow(() -> new IllegalStateException("Provider not found for " + username));
        model.addAttribute("providerId", p.getProviderId());
        model.addAttribute("companyName", p.getCompanyName());
        return "provider/services"; // thymeleaf template
    }

    @GetMapping("/provider/orders")
    public String orders() {
        return "provider/orders";
    }
}

