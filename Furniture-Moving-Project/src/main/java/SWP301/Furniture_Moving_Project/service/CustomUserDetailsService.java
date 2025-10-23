package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.model.AccountStatus;
import SWP301.Furniture_Moving_Project.model.UserAccount;
import SWP301.Furniture_Moving_Project.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userRepo;

    @Autowired(required = false)
    private JdbcTemplate jdbc; // optional; used if you have roles in DB

    public CustomUserDetailsService(UserAccountRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        UserAccount u = userRepo.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Not found"));

        if (u.getStatus() == AccountStatus.DELETED) throw new UsernameNotFoundException("Not found");
        if (u.getStatus() == AccountStatus.SUSPENDED) throw new DisabledException("Account suspended");

        String hash = u.getPasswordHash();
        if (hash == null || hash.isBlank()) throw new UsernameNotFoundException("Not found");

        List<String> roles = fetchRoleNamesSafely(u.getId());
        if (roles.isEmpty()) roles = List.of("USER");
        String[] auths = roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase(Locale.ROOT))
                .toArray(String[]::new);
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(auths);

        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPasswordHash())
                .authorities(authorities)
                .accountLocked(false)
                .disabled(false)
                .build();
    }

    private List<String> fetchRoleNamesSafely(Long userId) {
        if (jdbc == null || userId == null) return new ArrayList<>();
        try {
            List<String> names = jdbc.query(
                    "SELECT r.role_name FROM roles r JOIN user_roles ur ON ur.role_id = r.role_id WHERE ur.user_id = ?",
                    (rs, i) -> rs.getString(1),
                    userId
            );
            return names.stream().filter(s -> s != null && !s.isBlank()).collect(Collectors.toList());
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }
}
