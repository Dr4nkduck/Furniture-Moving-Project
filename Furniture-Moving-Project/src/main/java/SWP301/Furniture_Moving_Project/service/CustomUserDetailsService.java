package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.model.AuthCredential;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import SWP301.Furniture_Moving_Project.repository.AuthCredentialRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthCredentialRepository authCredentialRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    AuthCredentialRepository authCredentialRepository) {
        this.userRepository = userRepository;
        this.authCredentialRepository = authCredentialRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        // Cho phép nhập username hoặc email
        User user = resolveUser(login);

        // credentials
        AuthCredential auth = authCredentialRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("Credentials not found for: " + login));

        // roles -> authorities
        List<String> roleNames = userRepository.findRoleNamesByUserId(user.getUserId());
        List<GrantedAuthority> authorities = roleNames.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername()) // username “chuẩn” từ DB
                .password(auth.getPasswordHash())
                .authorities(authorities)
                .build();
    }

    /** Ưu tiên email nếu có ký tự '@', không phân biệt hoa thường; fallback sang cách còn lại. */
    private User resolveUser(String login) {
        boolean looksLikeEmail = login != null && login.contains("@");

        if (looksLikeEmail) {
            return userRepository.findByEmailIgnoreCase(login)
                    .or(() -> userRepository.findByUsernameIgnoreCase(login))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login));
        } else {
            return userRepository.findByUsernameIgnoreCase(login)
                    .or(() -> userRepository.findByEmailIgnoreCase(login))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login));
        }
    }
}
