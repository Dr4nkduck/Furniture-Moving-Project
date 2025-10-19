package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.controller.dto.ChangeStatusRequest;
import SWP301.Furniture_Moving_Project.controller.dto.UserAccountResponse;
import SWP301.Furniture_Moving_Project.model.AccountStatus;
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

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    /** List users with optional q= search, page/size pagination */
    @GetMapping
    public Page<UserAccountResponse> list(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer size) {
        return service.list(q, page, size);
    }

    /** Get one user by id */
    @GetMapping("/{id}")
    public UserAccountResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    /** Change status (ACTIVE/SUSPENDED/DELETED) */
    @PutMapping("/{id}/status")
    public UserAccountResponse changeStatus(@PathVariable Long id, @RequestBody ChangeStatusRequest body) {
        AccountStatus status = body.getStatus();
        return service.changeStatus(id, status);
    }

    /** Soft delete => set status = DELETED */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
