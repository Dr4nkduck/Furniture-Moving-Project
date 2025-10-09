package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.ActivityLog;
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

        Page<ActivityLog> pageData = (q == null || q.isBlank())
                ? logRepo.findAll(pageable)   // ✅ nếu không có từ khoá thì lấy hết
                : logRepo.search(q.trim(), pageable);

        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("pageData", pageData);

        return "superadmin/logs"; // ✅ khớp templates/superadmin/logs.html
    }

    // (Tuỳ chọn debug) /super/logs/count → trả số bản ghi.
    // @GetMapping("/logs/count") @ResponseBody
    // public String count() { return "count=" + logRepo.count(); }
}
