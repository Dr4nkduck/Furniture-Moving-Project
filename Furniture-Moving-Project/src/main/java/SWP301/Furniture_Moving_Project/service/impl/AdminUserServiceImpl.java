package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.UserAccountResponseDTO;
import SWP301.Furniture_Moving_Project.model.AccountStatus;
import SWP301.Furniture_Moving_Project.model.UserAccount;
import SWP301.Furniture_Moving_Project.repository.UserAccountRepository;
// (Tùy chọn) bật nếu bạn thêm RoleRepository để lấy primaryRole
// import SWP301.Furniture_Moving_Project.repository.RoleRepository;

import SWP301.Furniture_Moving_Project.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserAccountRepository repo;

    // (Tùy chọn) Nếu muốn lấy primaryRole, tạo RoleRepository và bật Autowired optional
    // @Autowired(required = false)
    // private RoleRepository roleRepo;

    public AdminUserServiceImpl(UserAccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public Page<UserAccountResponseDTO> list(String q, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page == null ? 0 : page,
                size == null ? 10 : size,
                Sort.by("id").descending());

        Page<UserAccount> pg;
        if (q == null || q.isBlank()) {
            pg = repo.findAll(pageable);
        } else {
            String s = q.trim();
            pg = repo.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                    s, s, s, s, pageable
            );
        }

        return pg.map(this::toDtoEntityOnly);
    }

    @Override
    public UserAccountResponseDTO get(Long id) {
        UserAccount u = repo.findById(id).orElseThrow();
        return toDtoEntityOnly(u);
    }

    @Override
    @Transactional
    public UserAccountResponseDTO changeStatus(Long id, AccountStatus status) {
        UserAccount u = repo.findById(id).orElseThrow();
        u.setStatus(status); // AccountStatusConverter sẽ lưu xuống DB dạng lowercase
        UserAccount saved = repo.save(u);
        return toDtoEntityOnly(saved);
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        changeStatus(id, AccountStatus.DELETED);
    }

    // ----------------- Helpers -----------------

    private UserAccountResponseDTO toDtoEntityOnly(UserAccount u) {
        String fullName = ((u.getFirstName() == null ? "" : u.getFirstName()) + " " +
                (u.getLastName()  == null ? "" : u.getLastName())).trim();

        // (Tùy chọn) Nếu đã tạo RoleRepository:
        // String primaryRole = (roleRepo != null) ? roleRepo.findPrimaryRoleByUserId(u.getId()) : null;

        return new UserAccountResponseDTO(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                fullName,
                u.getPhone(),
                null,            // đổi thành primaryRole nếu bạn bật RoleRepository ở trên
                u.getStatus(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}
