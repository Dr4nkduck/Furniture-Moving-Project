package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.model.*;
import SWP301.Furniture_Moving_Project.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final AuthCredentialRepository authCredentialRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository,
                               AuthCredentialRepository authCredentialRepository,
                               RoleRepository roleRepository,
                               UserRoleRepository userRoleRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authCredentialRepository = authCredentialRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Integer register(RegisterForm form) {
        // 1) Validate trùng
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        // 2) Tạo users
        User u = new User();
        u.setUsername(form.getUsername());
        u.setEmail(form.getEmail());
        u.setPhone(form.getPhone());
        u.setFirstName(form.getFirstName());
        u.setLastName(form.getLastName());
        u.setStatus("active");
        u.setCreatedAt(OffsetDateTime.now());
        u.setUpdatedAt(OffsetDateTime.now());
        u = userRepository.save(u);

        // 3) Tạo authentication với BCrypt
        AuthCredential cred = new AuthCredential();
        cred.setUserId(u.getUserId());
        cred.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        cred.setMfaEnabled(false);
        cred.setFailedAttempts(0);
        authCredentialRepository.save(cred);

        // 4) Gán role CUSTOMER
        Integer customerRoleId = roleRepository.findByRoleName("CUSTOMER")
                .map(Role::getRoleId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role CUSTOMER (hãy chạy seed DB)."));

        UserRole ur = new UserRole();
        ur.setUserId(u.getUserId());
        ur.setRoleId(customerRoleId);
        ur.setPrimary(true);
        userRoleRepository.save(ur);

        return u.getUserId();
    }
}
