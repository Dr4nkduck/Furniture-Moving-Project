package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ServicePackage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, Integer> {
 List<ServicePackage> findByIsActiveTrueOrderByNameAsc();
}
