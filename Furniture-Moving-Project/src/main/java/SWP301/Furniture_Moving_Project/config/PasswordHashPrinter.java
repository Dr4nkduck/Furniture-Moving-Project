package SWP301.Furniture_Moving_Project.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev") // only runs if you start with spring.profiles.active=dev (to be safe)
public class PasswordHashPrinter implements CommandLineRunner {
    private final PasswordEncoder encoder;
    public PasswordHashPrinter(PasswordEncoder encoder) { this.encoder = encoder; }

    @Override public void run(String... args) {
        String raw = "Admin@123";            // <-- choose your temp password
        String hash = encoder.encode(raw);   // e.g., $2a$10$...
        System.out.println("BCrypt for '"+raw+"': " + hash);
    }
}
