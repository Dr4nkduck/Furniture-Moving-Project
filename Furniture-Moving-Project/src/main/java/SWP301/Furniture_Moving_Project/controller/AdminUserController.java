package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ChangeStatusRequestDTO;
import SWP301.Furniture_Moving_Project.dto.UserAccountResponseDTO;
import SWP301.Furniture_Moving_Project.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService service;
    public AdminUserController(AdminUserService service) { this.service = service; }

    @GetMapping
    public Page<UserAccountResponseDTO> list(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return service.list(q, page, size);
    }

    @GetMapping("/{id}")
    public UserAccountResponseDTO get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}/status")
    public UserAccountResponseDTO changeStatus(@PathVariable Long id,
                                               @RequestBody ChangeStatusRequestDTO body) {
        return service.changeStatus(id, body.getStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
