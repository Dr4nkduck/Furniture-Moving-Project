// src/main/java/SWP301/Furniture_Moving_Project/model/AccountStatus.java
package SWP301.Furniture_Moving_Project.model;

/**
 * Trạng thái tài khoản:
 * - ACTIVE     : hoạt động bình thường
 * - SUSPENDED  : tạm khóa (không được đăng nhập)
 * - DELETED    : đã xóa/vô hiệu (không được đăng nhập)
 */
public enum AccountStatus {
    ACTIVE, SUSPENDED, DELETED
}
