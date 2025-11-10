// dto/PricingSaveRequestDTO.java
package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.util.List;

public class PricingSaveRequestDTO {
    public Integer providerId;
    public Integer packageId;

    public String packageName;      // snapshot muốn lưu (nếu để trống, sẽ copy từ ServicePackage.name)
    public BigDecimal pricePerKm;

    public List<FurniturePriceDTO> furniturePrices;
}
