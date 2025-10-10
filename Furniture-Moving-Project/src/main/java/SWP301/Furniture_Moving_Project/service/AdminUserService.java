package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.controller.dto.UserAccountResponse;
import SWP301.Furniture_Moving_Project.model.AccountStatus;
import org.springframework.data.domain.Page;

/**
 * Service cho quản trị tài khoản người dùng (Admin).
 * - Liệt kê + tìm kiếm (phân trang)
 * - Xem chi tiết 1 user
 * - Đổi trạng thái tài khoản (ACTIVE/SUSPENDED/DELETED)
 * - Soft delete (đặt trạng thái = DELETED)
 */
public interface AdminUserService {

    /**
     * Liệt kê người dùng với tìm kiếm tùy chọn (username/email/first_name/last_name).
     *
     * @param q     Từ khóa tìm kiếm (có thể null/blank)
     * @param page  Trang (0-based)
     * @param size  Kích thước trang
     * @return Page<UserAccountResponse>
     */
    Page<UserAccountResponse> list(String q, Integer page, Integer size);

    /**
     * Lấy thông tin 1 người dùng.
     *
     * @param id ID người dùng
     * @return UserAccountResponse
     */
    UserAccountResponse get(Long id);

    /**
     * Đổi trạng thái tài khoản.
     *
     * @param id     ID người dùng
     * @param status Trạng thái mới (ACTIVE/SUSPENDED/DELETED)
     * @return UserAccountResponse sau khi cập nhật
     */
    UserAccountResponse changeStatus(Long id, AccountStatus status);

    /**
     * Soft delete: đặt trạng thái = DELETED.
     *
     * @param id ID người dùng
     */
    void softDelete(Long id);
}
