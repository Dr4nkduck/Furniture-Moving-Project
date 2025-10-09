package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProviderDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProviderApiController {

    private final ProviderRepository providerRepository;

    public ProviderApiController(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<Provider> entities = providerRepository.findAll();

        List<ProviderDTO> data = entities.stream()
                .map(p -> new ProviderDTO(
                        p.getProviderId(),
                        p.getCompanyName(),
                        p.getRating()
                ))
                .collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }
}
