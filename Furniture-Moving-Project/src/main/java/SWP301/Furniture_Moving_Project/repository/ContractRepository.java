package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Integer> {
    boolean existsByUser_UserIdAndStatus(Integer userId, String status); // <— dùng userId
}
