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

    // Dùng DelegatingPasswordEncoder để có thể lưu {noop}... trong DB cho dễ seed
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
                .requestMatchers("/", "/homepage", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasAnyRole("CUSTOMER","PROVIDER","STAFF","SUPPORT") // cho cả provider vào /user
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/perform_login") // khớp action của form
                .successHandler(successHandler)       // chuyển trang theo role
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/homepage")
                .permitAll()
            );
        return http.build();
    }
}
