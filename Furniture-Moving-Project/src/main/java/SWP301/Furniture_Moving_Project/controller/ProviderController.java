package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderDTO;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProviderController {

    private final ProviderRepository providerRepository;

    public ProviderController(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @GetMapping("/available")
    public Map<String, Object> getAvailableProviders() {
        List<ProviderDTO> list = providerRepository.findAvailableProviders();
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", list);
        return body;
    }
}

