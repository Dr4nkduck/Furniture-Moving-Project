// service/ProviderPricingService.java (interface)
package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.PackagePricingDetailDTO;
import SWP301.Furniture_Moving_Project.dto.PackageOptionDTO;
import SWP301.Furniture_Moving_Project.dto.PricingSaveRequestDTO;

import java.util.List;

public interface ProviderPricingService {
    List<PackageOptionDTO> listPackages(Integer providerId);
    PackagePricingDetailDTO getPackageDetail(Integer providerId, Integer packageId);
    void savePackagePricing(PricingSaveRequestDTO req);
}
