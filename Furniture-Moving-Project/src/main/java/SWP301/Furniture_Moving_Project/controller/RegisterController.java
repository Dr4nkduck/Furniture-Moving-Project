package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.RegisterForm;
import SWP301.Furniture_Moving_Project.service.RegistrationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegisterController {

    private final RegistrationService registrationService;

    public RegisterController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String showForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "accountmanage/register";
    }

    @PostMapping("/register")
    public String submit(@ModelAttribute("form") RegisterForm form, Model model) {
        try {
            registrationService.register(form);
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "accountmanage/register";
        }
    }
}
