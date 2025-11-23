package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.ProductPriceItemDTO;
import SWP301.Furniture_Moving_Project.model.ProductPrice;
import SWP301.Furniture_Moving_Project.repository.ProductPriceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductPriceService {

    private final ProductPriceRepository productPriceRepository;

    public ProductPriceService(ProductPriceRepository productPriceRepository) {
        this.productPriceRepository = productPriceRepository;
    }

    public List<ProductPriceItemDTO> getActiveItemsForAiQuote() {
        List<ProductPrice> entities = productPriceRepository.findByActiveTrue();
        return entities.stream()
                .map(ProductPriceItemDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
