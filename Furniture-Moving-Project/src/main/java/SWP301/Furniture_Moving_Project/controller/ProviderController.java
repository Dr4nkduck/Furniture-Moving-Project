package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/provider")
public class ProviderController {

    @GetMapping("")
    public String providerHome() { return "provider";  // => providers.html
    }
}
