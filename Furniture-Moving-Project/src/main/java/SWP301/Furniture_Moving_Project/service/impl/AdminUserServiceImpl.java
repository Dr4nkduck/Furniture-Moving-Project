package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.UserAccountResponseDTO;
import SWP301.Furniture_Moving_Project.model.AccountStatus;
import SWP301.Furniture_Moving_Project.model.UserAccount;
import SWP301.Furniture_Moving_Project.repository.UserAccountRepository;
// import SWP301.Furniture_Moving_Project.repository.RoleRepository; // optional
import SWP301.Furniture_Moving_Project.service.AdminUserService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserAccountRepository repo;
    // private final RoleRepository roleRepo; // optional

    public AdminUserServiceImpl(UserAccountRepository repo /*, RoleRepository roleRepo */) {
        this.repo = repo;
        // this.roleRepo = roleRepo;
    }

    @Override
    public Page<UserAccountResponseDTO> list(String q, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page == null ? 0 : page, size == null ? 10 : size, Sort.by("id").descending());
        Page<UserAccount> pg = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                q.trim(), q.trim(), q.trim(), q.trim(), pageable);
        return pg.map(this::toDto);
    }

    @Override
    public UserAccountResponseDTO get(Long id) {
        return toDto(repo.findById(id).orElseThrow());
    }

    @Override
    @Transactional
    public UserAccountResponseDTO changeStatus(Long id, AccountStatus status) {
        UserAccount u = repo.findById(id).orElseThrow();
        u.setStatus(status);           // converter persists lowercase strings in DB
        return toDto(repo.save(u));
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        changeStatus(id, AccountStatus.DELETED);
    }

    private UserAccountResponseDTO toDto(UserAccount u) {
        String fullName = ((u.getFirstName() == null ? "" : u.getFirstName()) + " " +
                (u.getLastName()  == null ? "" : u.getLastName())).trim();
        // String role = (roleRepo != null) ? roleRepo.findPrimaryRoleByUserId(u.getId()) : null;
        return new UserAccountResponseDTO(
                u.getId(), u.getUsername(), u.getEmail(), fullName, u.getPhone(),
                null, /* role */
                u.getStatus(), u.getCreatedAt(), u.getUpdatedAt()
        );
    }
}
