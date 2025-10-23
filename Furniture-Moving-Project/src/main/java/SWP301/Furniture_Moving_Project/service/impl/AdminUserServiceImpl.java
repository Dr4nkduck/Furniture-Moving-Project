package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.UserAccountResponseDTO;
import SWP301.Furniture_Moving_Project.model.AccountStatus;
import SWP301.Furniture_Moving_Project.model.UserAccount;
import SWP301.Furniture_Moving_Project.repository.UserAccountRepository;
import SWP301.Furniture_Moving_Project.service.AdminUserService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserAccountRepository repo;

    public AdminUserServiceImpl(UserAccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public Page<UserAccountResponseDTO> list(String q, Integer page, Integer size) {
        return list(q, page, size, null, null);
    }

    @Override
    public Page<UserAccountResponseDTO> list(String q, Integer page, Integer size,
                                             OffsetDateTime createdFrom, OffsetDateTime createdTo) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0) ? 10 : size;
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "id"));
        String term = (q == null) ? "" : q.trim();
        Page<UserAccount> pg = repo.searchAdmin(term, createdFrom, createdTo, pageable);
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
        u.setStatus(status);
        return toDto(repo.save(u));
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        changeStatus(id, AccountStatus.DELETED);
    }

    private UserAccountResponseDTO toDto(UserAccount u) {
        String full = ((u.getFirstName() == null ? "" : u.getFirstName()) + " " + (u.getLastName() == null ? "" : u.getLastName())).trim();
        String phone = u.getPhone() == null ? "" : u.getPhone();
        return new UserAccountResponseDTO(
                u.getId(), u.getUsername(), u.getEmail(), full, phone,
                null, u.getStatus(), u.getCreatedAt(), u.getUpdatedAt()
        );
    }
}
