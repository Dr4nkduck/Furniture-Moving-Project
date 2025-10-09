package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.repository.ActivityLogRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/super")
public class SuperAdminController {

    private final ActivityLogRepository logRepo;

    public SuperAdminController(ActivityLogRepository logRepo) {
        this.logRepo = logRepo;
    }

    @GetMapping("/logs")
    public String logs(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        var pageData = logRepo.search(q, pageable);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("pageData", pageData);
        return "superadmin/logs";
    }

    @GetMapping("")
    public String home() {
        return "redirect:/super/logs";
    }
}
