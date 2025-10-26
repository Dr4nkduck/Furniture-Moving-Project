package SWP301.Furniture_Moving_Project.controller;


import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.PrivateKey;
import java.util.List;

@Controller
@RequestMapping("/shipping-providers")
public class ShippingProvidersController {

    private final ProviderRepository providerRepository;
    public ShippingProvidersController(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @GetMapping
    public String listProviders(Model model) {
        // Fetch all providers from the database
        List<Provider> providers = providerRepository.findAll();

        // Add providers to the model to be used in the view
        model.addAttribute("providers", providers);

        // Return the view name
        return "shipping-providers/list";
    }
}
