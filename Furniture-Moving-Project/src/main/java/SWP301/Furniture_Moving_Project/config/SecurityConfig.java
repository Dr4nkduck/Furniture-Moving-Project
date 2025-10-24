package SWP301.Furniture_Moving_Project.config;

import SWP301.Furniture_Moving_Project.service.CustomUserDetailsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final RoleBasedAuthSuccessHandler successHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          RoleBasedAuthSuccessHandler successHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
    }

    /**
     * Dùng DelegatingPasswordEncoder để tương thích cả {noop}, bcrypt, v.v.
     * (Hợp với dữ liệu seed đang để {noop}Admin@123, {noop}User@123, …)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authenticationProvider(daoAuthenticationProvider())
            .authorizeHttpRequests(auth -> auth

                // --- Public pages
                .requestMatchers("/", "/homepage", "/login", "/register", "/forgot/**").permitAll()

                // --- Static assets
                .requestMatchers(
                    "/css/**", "/js/**", "/images/**",
                    "/accountmanage/**", "/homepage/**", "/chatbot/**",
                    "/dashboard/**",           // đúng thư mục
                    "/dashbooard/**",          // (giữ để tương thích nếu còn typo thư mục)
                    "/admin/js/**", "/admin/css/**",
                    "/superadmin/**"
                ).permitAll()

                // --- Open API
                .requestMatchers(HttpMethod.GET, "/api/providers").permitAll()

                // --- Admin pages (bao gồm 2 URL bạn yêu cầu)
                .requestMatchers(
                    "/admin/providers/stats", "/admin/providers/stats/**",
                    "/admin/customers/trends", "/admin/customers/trends/**",
                    "/admin/**"
                ).hasAnyRole("ADMIN", "SUPER_ADMIN")

                // --- User/Provider areas (giữ nguyên nếu bạn cần)
                .requestMatchers("/user/**").hasRole("CUSTOMER")
                .requestMatchers("/provider/**").hasRole("PROVIDER")

                // --- Anything else requires auth
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/homepage")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    /** In ra bcrypt để bạn tiện copy seed DB nếu muốn dùng bcrypt thay {noop} */
    @Component
    static class PrintHashOnce implements CommandLineRunner {
        private final PasswordEncoder encoder;
        PrintHashOnce(PasswordEncoder encoder){ this.encoder = encoder; }
        @Override public void run(String... args) {
            System.out.println("Sample BCrypt for 'Admin@123': " + encoder.encode("Admin@123"));
        }
    }
}
