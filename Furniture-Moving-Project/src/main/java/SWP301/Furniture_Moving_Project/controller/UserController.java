package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {
    @GetMapping("/user")
    public String userHome() {
        return "user";
    }
}
