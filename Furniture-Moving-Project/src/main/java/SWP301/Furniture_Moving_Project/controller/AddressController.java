package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.DTO.AddressDTO;
import SWP301.Furniture_Moving_Project.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = "*")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody AddressDTO dto) {
        Integer id = addressService.create(dto);
        Map<String, Object> data = new HashMap<>();
        data.put("addressId", id);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }
}
