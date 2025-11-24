package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.ProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, Integer> {

    // chỉ lấy những dòng đang active
    List<ProductPrice> findByActiveTrue();
}
