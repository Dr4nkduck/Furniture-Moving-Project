package SWP301.Furniture_Moving_Project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import SWP301.Furniture_Moving_Project.model.UserRole;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> { }
