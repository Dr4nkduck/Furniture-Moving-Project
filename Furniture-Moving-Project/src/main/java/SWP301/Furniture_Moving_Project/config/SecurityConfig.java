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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;



@EnableMethodSecurity
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
                    .requestMatchers("/provider/invoices/**").hasAnyRole("PROVIDER","ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/service-requests/**").hasAnyRole("PROVIDER","ADMIN")
                .requestMatchers("/", "/homepage", "/login", "/register", "/distanceCalculation",
                        "/requestservice",                    // ✅ NEW: trang tạo yêu cầu (view Thymeleaf)
                        "/distanceCalculation",              // ✅ NEW: trang tính quãng đường (view Thymeleaf)
                        "/requestService/**",                // ✅ NEW: static CSS/JS của trang request
                        "/distanceCalculation/**",           // ✅ NEW: static (nếu có) của trang distance
                                 "/forgot/**",
                                 "/css/**", "/js/**", "/images/**",
                                 "/accountmanage/**", "/homepage/**", "/chatbot/**",
                                 "/superadmin/**",
                                 "/dashbooard/**" ,
                        "/approval/**"

                ).permitAll()

                    .requestMatchers("/approveApplication/css/**", "/approveApplication/js/**").permitAll()
                    .requestMatchers("/approveApplication").authenticated()

                    .requestMatchers("/super/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
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
}
