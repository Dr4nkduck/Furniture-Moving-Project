package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.util.List;

public record QuickRequestDTO(
        Integer customerId,
        Integer providerId,           // có thể null
        Integer pickupAddressId,
        Integer deliveryAddressId,
        String  preferredDate,        // "yyyy-MM-dd"
        BigDecimal totalCost,         // tổng tiền đã tính ở FE
        List<ItemDTO> furnitureItems  // tuỳ chọn
) {
    public record ItemDTO(
            String itemType, String size, Integer quantity, Boolean isFragile, String specialHandling
    ){}
}
