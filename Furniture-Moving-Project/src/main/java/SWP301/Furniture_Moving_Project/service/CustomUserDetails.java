// src/main/java/SWP301/Furniture_Moving_Project/service/CustomUserDetails.java
package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.model.AccountStatus;
import SWP301.Furniture_Moving_Project.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class CustomUserDetails implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final User user;
    private final String passwordHash;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(User user, String passwordHash, List<GrantedAuthority> authorities) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
    }

    public Integer getUserId() {
        return user.getUserId();
    }

    public User getUser() {
        return user;
    }

    public AccountStatus getAccountStatus() {
        return user.getAccountStatus();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return ns(user.getAccountStatus()) != SWP301.Furniture_Moving_Project.model.AccountStatus.SUSPENDED;
    }

    @Override
    public boolean isEnabled() {
        return ns(user.getAccountStatus()) == SWP301.Furniture_Moving_Project.model.AccountStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CustomUserDetails that))
            return false;
        return Objects.equals(this.getUsername(), that.getUsername());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getUsername());
    }

    @Override
    public String toString() {
        return "CustomUserDetails{username='" + getUsername() + "', status=" + user.getAccountStatus() + "}";
    }

    private static SWP301.Furniture_Moving_Project.model.AccountStatus ns(
            SWP301.Furniture_Moving_Project.model.AccountStatus st) {
        return st == null ? SWP301.Furniture_Moving_Project.model.AccountStatus.ACTIVE : st;
    }
}
