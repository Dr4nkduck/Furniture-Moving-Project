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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1) user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 2) credentials
        AuthCredential auth = authCredentialRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("Credentials not found for user: " + username));

        // 3) roles -> authorities (ROLE_*)
        List<String> roleNames = userRepository.findRoleNamesByUserId(user.getUserId()); // ví dụ ["ADMIN"] hoặc ["CUSTOMER"]
        List<GrantedAuthority> authorities = roleNames.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());

        // 4) trả UserDetails (password_hash trong DB)
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(auth.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
