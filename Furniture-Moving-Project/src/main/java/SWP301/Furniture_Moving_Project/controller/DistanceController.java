package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DistanceController {
    @GetMapping("/distanceCalculation")
    public String page() {
        // trả về: templates/distanceCalculation/distanceCalculation.html
        return "distanceCalculation/distanceCalculation";
    }
}

