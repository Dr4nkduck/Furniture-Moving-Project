package SWP301.Furniture_Moving_Project.config;

import SWP301.Furniture_Moving_Project.service.CustomUserDetailsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }


    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authenticationProvider(daoAuthenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/homepage", "/login", "/register",
                                 "/forgot/**",
                                 "/css/**", "/js/**", "/images/**",
                                 "/accountmanage/**", "/homepage/**", "/chatbot/**",
                                 "/superadmin/**",
                                 "/dashbooard/**"                 // ✅ static của superadmin (css/js)
                ).permitAll()
                .requestMatchers("/super/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                    .requestMatchers("/accountmanagement").hasRole("ADMIN")
                    .requestMatchers("/api/admin/analytics/**").hasRole("ADMIN")
                    .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasRole("CUSTOMER")
                .requestMatchers("/provider/**").hasRole("PROVIDER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/providers").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/homepage")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll());
        return http.build();
    }

    @Component
    class PrintHashOnce implements CommandLineRunner {
        private final PasswordEncoder encoder;
        PrintHashOnce(PasswordEncoder encoder){ this.encoder = encoder; }
        @Override public void run(String... args) {
            String raw = "Admin@123";
            System.out.println("BCrypt: " + encoder.encode(raw));
        }
    }
}
