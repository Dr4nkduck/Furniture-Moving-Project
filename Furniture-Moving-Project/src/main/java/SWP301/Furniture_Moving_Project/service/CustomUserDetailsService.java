package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.model.AuthCredential;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import SWP301.Furniture_Moving_Project.repository.AuthCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;
    private final AuthCredentialRepository authRepo;

    @Autowired(required = false)
    private JdbcTemplate jdbc; // optional để lấy role từ DB

    public CustomUserDetailsService(UserRepository userRepo,
                                    AuthCredentialRepository authRepo) {
        this.userRepo = userRepo;
        this.authRepo = authRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // 1) Tìm user theo email nếu có '@', ngược lại theo username
        User u;
        if (usernameOrEmail != null && usernameOrEmail.contains("@")) {
            u = userRepo.findByEmail(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("Not found"));
        } else {
            u = userRepo.findByUsername(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("Not found"));
        }

        // 2) Kiểm tra trạng thái
        String st = (u.getStatus() == null ? "active" : u.getStatus().toLowerCase(Locale.ROOT));
        if ("deleted".equals(st)) throw new UsernameNotFoundException("Not found");
        if ("suspended".equals(st)) throw new DisabledException("Account suspended");

        // 3) Lấy hash từ bảng authentication
        AuthCredential cred = authRepo.findByUserId(u.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("Not found"));

        String hash = cred.getPasswordHash();
        if (hash == null || hash.isBlank()) throw new UsernameNotFoundException("Not found");

        // 4) Khoá tạm thời?
        OffsetDateTime now = OffsetDateTime.now();
        if (cred.getAccountLockedUntil() != null && now.isBefore(cred.getAccountLockedUntil())) {
            throw new LockedException("Account locked until " + cred.getAccountLockedUntil());
        }

        // 5) Lấy roles
        List<String> roles = fetchRoleNamesSafely(u.getUserId());
        if (roles.isEmpty()) roles = List.of("CUSTOMER"); // fallback

        String[] auths = roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase(Locale.ROOT))
                .toArray(String[]::new);
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(auths);

        // 6) Trả UserDetails
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(hash) // hỗ trợ {noop}.., {bcrypt}.. nhờ DelegatingPasswordEncoder
                .authorities(authorities)
                .accountLocked(false)
                .disabled(false)
                .build();
    }

    private List<String> fetchRoleNamesSafely(Integer userId) {
        if (jdbc == null || userId == null) return new ArrayList<>();
        try {
            return jdbc.query(
                """
                SELECT r.role_name
                FROM user_roles ur
                JOIN roles r ON r.role_id = ur.role_id
                WHERE ur.user_id = ?
                """,
                (rs, i) -> rs.getString(1),
                userId
            );
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }
}
