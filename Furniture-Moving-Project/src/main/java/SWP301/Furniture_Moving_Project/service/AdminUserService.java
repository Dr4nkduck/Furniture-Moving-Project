package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.UserAccountResponseDTO;
import SWP301.Furniture_Moving_Project.model.AccountStatus;
import org.springframework.data.domain.Page;

public interface AdminUserService {
    Page<UserAccountResponseDTO> list(String q, Integer page, Integer size);
    UserAccountResponseDTO get(Long id);
    UserAccountResponseDTO changeStatus(Long id, AccountStatus status);
    void softDelete(Long id);
}
