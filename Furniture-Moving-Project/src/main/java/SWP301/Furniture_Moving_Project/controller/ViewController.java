package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    /** URL chính: http://localhost:8080/request */
    @GetMapping("/request")
    public String requestPage() {
        return "request/request"; // templates/request/request.html
    }

    /** Giữ url cũ: chuyển hướng về /request */
    @GetMapping({"/requestservice", "/request-service"})
    public String legacyRequestUrls() {
        return "redirect:/request";
    }

    // KHÔNG map "/" ở đây để tránh đụng HomeController
}
