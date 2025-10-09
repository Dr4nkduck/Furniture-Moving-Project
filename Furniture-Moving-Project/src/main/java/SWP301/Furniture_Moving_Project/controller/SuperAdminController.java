package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.ActivityLog;
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.repository.ActivityLogRepository;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/super")
public class SuperAdminController {

    private final ActivityLogRepository logRepo;
    private final UserRepository userRepo;

    public SuperAdminController(ActivityLogRepository logRepo, UserRepository userRepo) {
        this.logRepo = logRepo;
        this.userRepo = userRepo;
    }

    /* ========== Dashboard (sau login) ========== */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalUsers  = userRepo.count();
        long totalAdmins = userRepo.countAdmins();
        long totalLogs   = logRepo.count();

        var latest5 = logRepo.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "timestamp"))
        ).getContent();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalAdmins", totalAdmins);
        model.addAttribute("totalLogs", totalLogs);
        model.addAttribute("latestLogs", latest5);
        return "superadmin/dashboard"; // templates/superadmin/dashboard.html
    }

    /* ========== Logs (THÊM rolesMap) ========== */
    @GetMapping("/logs")
    public String logs(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<ActivityLog> pageData = (q == null || q.isBlank())
                ? logRepo.findAll(pageable)
                : logRepo.search(q.trim(), pageable);

        // === Build rolesMap: userId -> "SUPER_ADMIN, ADMIN, ..." (ưu tiên sắp xếp đẹp mắt) ===
        Map<Integer, String> rolesMap = new HashMap<>();
        // Lấy userId distinct (bỏ null)
        Set<Integer> userIds = pageData.getContent().stream()
                .map(ActivityLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (Integer uid : userIds) {
            List<String> roles = userRepo.findRoleNamesByUserId(uid);
            // Ưu tiên SUPER_ADMIN > ADMIN > PROVIDER > CUSTOMER > khác
            List<String> ordered = roles.stream()
                    .sorted(Comparator.comparingInt(role -> {
                        return switch (role) {
                            case "SUPER_ADMIN" -> 0;
                            case "ADMIN"       -> 1;
                            case "PROVIDER"    -> 2;
                            case "CUSTOMER"    -> 3;
                            default            -> 9;
                        };
                    })).toList();
            rolesMap.put(uid, String.join(", ", ordered));
        }

        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("pageData", pageData);
        model.addAttribute("rolesMap", rolesMap); // ✅ đưa vào view

        return "superadmin/logs";
    }

    /* ========== Users (tất cả user) ========== */
    @GetMapping("/users")
    public String users(@RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "10") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> pageData = userRepo.findAll(pageable);
        model.addAttribute("pageData", pageData);
        return "superadmin/users"; // templates/superadmin/users.html
    }

    /* ========== Admins (ADMIN + SUPER_ADMIN) ========== */
    @GetMapping("/admins")
    public String admins(@RequestParam(value = "page", defaultValue = "0") int page,
                         @RequestParam(value = "size", defaultValue = "10") int size,
                         Model model) {
        // KHÔNG truyền Sort vì native query đã ORDER BY created_at DESC
        Pageable pageable = PageRequest.of(page, size);

        Page<User> pageData = userRepo.findAdmins(pageable);
        model.addAttribute("pageData", pageData);
        return "superadmin/admins"; // templates/superadmin/admins.html
    }
}
