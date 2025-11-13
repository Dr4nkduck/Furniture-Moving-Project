// src/main/java/SWP301/Furniture_Moving_Project/service/CustomUserDetailsService.java
package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.model.AccountStatus;
import SWP301.Furniture_Moving_Project.model.User;
import SWP301.Furniture_Moving_Project.model.AuthCredential;
import SWP301.Furniture_Moving_Project.repository.UserRepository;
import SWP301.Furniture_Moving_Project.repository.AuthCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthCredentialRepository authCredentialRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository,
            AuthCredentialRepository authCredentialRepository) {
        this.userRepository = userRepository;
        this.authCredentialRepository = authCredentialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        // Cho phép nhập username hoặc email
        User user = resolveUser(login);

        SWP301.Furniture_Moving_Project.model.AccountStatus st = user.getAccountStatus();

        // Nếu tài khoản bị SUSPENDED → chặn như locked
        if (st == SWP301.Furniture_Moving_Project.model.AccountStatus.SUSPENDED) {
            throw new org.springframework.security.authentication.LockedException("Account suspended");
        }

        // Nếu tài khoản bị DELETED → chặn như disabled
        if (st == SWP301.Furniture_Moving_Project.model.AccountStatus.DELETED) {
            throw new org.springframework.security.authentication.DisabledException("Account deleted");
        }

        // Lấy password hash từ bảng xác thực (AuthCredential) — giữ nguyên luồng của
        // bạn
        AuthCredential auth = authCredentialRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại."));

        // Lấy role → authority — giữ nguyên cách của bạn
        List<String> roleNames = userRepository.findRoleNamesByUserId(user.getUserId());
        List<GrantedAuthority> authorities = roleNames.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());

        // CustomUserDetails là class của dự án bạn, giữ nguyên
        return new CustomUserDetails(user, auth.getPasswordHash(), authorities);
    }

    // Giữ nguyên logic phân giải username/email, dùng đúng repository hiện có
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
