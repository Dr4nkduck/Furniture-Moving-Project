// dto/PackageDetailDTO.java
package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.util.List;

public class PackagePricingDetailDTO {
    public String packageNameSnapshot;
    public BigDecimal pricePerKm;
    public List<FurniturePriceDTO> furniturePrices;
}
