package SWP301.Furniture_Moving_Project.config;

import SWP301.Furniture_Moving_Project.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authenticationProvider(daoAuthenticationProvider())
                .authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/homepage", "/login", "/register",
                     "/forgot/**",
                     "/css/**", "/js/**", "/images/**",
                     "/accountmanage/**", "/homepage/**", "/chatbot/**")
        .permitAll()
    .requestMatchers("/super/**").hasRole("SUPER_ADMIN")
    // Cho phép cả SUPER_ADMIN lẫn ADMIN vào /admin/**
    .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
    .requestMatchers("/user/**").hasRole("CUSTOMER")
    .requestMatchers("/provider/**").hasRole("PROVIDER")
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
                        .permitAll())
                .headers(headers -> headers.cacheControl(cache -> {
                }));

        return http.build();
    }

}
