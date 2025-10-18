package SWP301.Furniture_Moving_Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ChatbotController {
    @GetMapping("/chatbot")
    public String chatbotPage() {
        return "chatbot/chatbot"; // templates/chatbot/chatbot.html
    }
}
