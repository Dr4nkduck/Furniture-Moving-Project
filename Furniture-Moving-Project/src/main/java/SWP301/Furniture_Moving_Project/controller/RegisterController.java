package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.model.RegisterForm;
import SWP301.Furniture_Moving_Project.service.RegistrationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private final RegistrationService registrationService;

    public RegisterController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String registerForm(Model model, @RequestParam(value = "error", required = false) String error) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        model.addAttribute("error", error);
        return "accountmanage/register";
    }

    @PostMapping("/register")
    public String doRegister(@ModelAttribute("form") RegisterForm form, RedirectAttributes ra) {
        try {
            registrationService.register(form);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("form", form);
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/register";
        } catch (Exception ex) {
            ra.addFlashAttribute("form", form);
            ra.addFlashAttribute("error", "Đăng ký thất bại. Vui lòng thử lại.");
            return "redirect:/register";
        }
    }
}
