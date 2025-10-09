package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to serve HTML pages (Views)
 * Note: This is @Controller, NOT @RestController
 */
@Controller
public class ViewController {
    
    /**
     * Request Service page - Create new transport request
     * URL: http://localhost:8081/requestservice
     */
    @GetMapping("/requestservice")
    public String requestServicePage() {
        return "requestservice";  // Returns templates/requestservice.html
    }
    
    /**
     * Alternative URL with dash
     * URL: http://localhost:8081/request-service
     */
    @GetMapping("/request-service")
    public String requestServicePageAlt() {
        return "requestservice";
    }
    
    /**
     * If your file is in user folder: templates/user/requestservice.html
     * Uncomment this if needed:
     */
    /*
    @GetMapping("/user/requestservice")
    public String userRequestServicePage() {
        return "user/requestservice";
    }
    */
}