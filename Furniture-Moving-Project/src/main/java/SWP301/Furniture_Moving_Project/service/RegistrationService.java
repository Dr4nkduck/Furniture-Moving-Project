package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.RegisterForm;
import SWP301.Furniture_Moving_Project.model.*;
import SWP301.Furniture_Moving_Project.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.math.BigDecimal;   // ✅ thêm import cho BigDecimal

@Service
public class RegistrationService {

    private static final AccountStatus status = AccountStatus.ACTIVE;
    private final UserRepository userRepository;
    private final AuthCredentialRepository authCredentialRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProviderRepository providerRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository,
                               AuthCredentialRepository authCredentialRepository,
                               RoleRepository roleRepository,
                               UserRoleRepository userRoleRepository,
                               ProviderRepository providerRepository,
                               CustomerRepository customerRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authCredentialRepository = authCredentialRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.providerRepository = providerRepository;
        this.customerRepository = customerRepository;
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
        if (form.getRole() == null || form.getRole().isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn vai trò");
        }

        final String pickedRole = form.getRole().trim().toUpperCase();
        if (!pickedRole.equals("CUSTOMER") && !pickedRole.equals("PROVIDER")) {
            throw new IllegalArgumentException("Vai trò không hợp lệ");
        }

        // 2) Tạo users
        User u = new User();
        u.setUsername(form.getUsername());
        u.setEmail(form.getEmail());
        u.setPhone(form.getPhone());
        u.setFirstName(form.getFirstName());
        u.setLastName(form.getLastName());
        u.setAccountStatus(status);
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

        // 4) Gán role do người dùng chọn (PRIMARY)
        Integer roleId = roleRepository.findByRoleName(pickedRole)
                .map(Role::getRoleId)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy role " + pickedRole + " (hãy chạy seed DB)."));

        UserRole ur = new UserRole();
        ur.setUserId(u.getUserId());
        ur.setRoleId(roleId);
        ur.setPrimary(true);
        userRoleRepository.save(ur);

        // 5) Tạo bản ghi domain CUSTOMER / PROVIDER tương ứng
        if ("CUSTOMER".equals(pickedRole)) {
            Customer customer = new Customer();
            customer.setUser(u);
            customer.setLoyaltyPoints(0);
            customerRepository.save(customer);

        } else if ("PROVIDER".equals(pickedRole)) {
            Provider provider = new Provider();
            provider.setUser(u);

            // Tên công ty: ghép từ first + last name hoặc fallback theo username
            String fullName = ((form.getFirstName() == null ? "" : form.getFirstName()) + " " +
                    (form.getLastName() == null ? "" : form.getLastName())).trim();
            String companyName = fullName.isEmpty() ? ("Provider " + u.getUsername()) : fullName;
            provider.setCompanyName(companyName);

            provider.setVerificationStatus("pending");
            provider.setRating(BigDecimal.ZERO);  // ✅ dùng BigDecimal
            provider.setTotalReviews(0);

            providerRepository.save(provider);
        }

        return u.getUserId();
    }
}
