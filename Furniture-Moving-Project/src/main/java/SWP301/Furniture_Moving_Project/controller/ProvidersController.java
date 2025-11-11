package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ReviewRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class ProvidersController {

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public ProvidersController(ProviderRepository providerRepository, UserRepository userRepository, ReviewRepository reviewRepository) {
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/providers")
    public String providers(Model model) {
        List<Provider> entities = providerRepository.findAll();

        List<ProviderDTO> providers = entities.stream()
                .map(p -> new ProviderDTO(
                        p.getProviderId(),
                        p.getCompanyName(),
                        p.getRating()
                ))
                .collect(Collectors.toList());

        model.addAttribute("providers", providers);

        addCurrentUser(model);

        return "providers";
    }

    @GetMapping("/providers/{id}")
    public String providerDetail(@PathVariable("id") Integer id, Model model) {
        Optional<Provider> providerOpt = providerRepository.findById(id);
        if (providerOpt.isEmpty()) {
            return "redirect:/providers";
        }
        Provider provider = providerOpt.get();
        model.addAttribute("provider", provider);
        model.addAttribute("reviews", reviewRepository.findByProviderIdOrderByCreatedAtDesc(id));

        addCurrentUser(model);

        return "provider-detail";
    }

    private void addCurrentUser(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Optional<User> userOpt = userRepository.findByUsername(auth.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("currentUser", user);
                model.addAttribute("isLoggedIn", true);
                return;
            }
        }
        model.addAttribute("isLoggedIn", false);
    }
}
