package SWP301.Furniture_Moving_Project.controller;

import SWP301.Furniture_Moving_Project.dto.ProductPriceItemDTO;
import SWP301.Furniture_Moving_Project.service.ProductPriceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiQuoteApiController {

    private final ProductPriceService productPriceService;

    public AiQuoteApiController(ProductPriceService productPriceService) {
        this.productPriceService = productPriceService;
    }

    /**
     * Trả danh sách giá cho "Báo giá bằng AI".
     * JSON format:
     * [
     *   { "name": "Giường đơn (Nhỏ)", "price": 250000 },
     *   ...
     * ]
     */
    @GetMapping("/products-price")
    public List<ProductPriceItemDTO> getProductsPrice() {
        return productPriceService.getActiveItemsForAiQuote();
    }
}
