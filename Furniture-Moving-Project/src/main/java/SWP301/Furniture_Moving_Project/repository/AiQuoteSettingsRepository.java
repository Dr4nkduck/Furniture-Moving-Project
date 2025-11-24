package SWP301.Furniture_Moving_Project.repository;

import SWP301.Furniture_Moving_Project.model.AiQuoteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiQuoteSettingsRepository extends JpaRepository<AiQuoteSettings, Integer> {

    AiQuoteSettings findTopByOrderByIdAsc();
}
