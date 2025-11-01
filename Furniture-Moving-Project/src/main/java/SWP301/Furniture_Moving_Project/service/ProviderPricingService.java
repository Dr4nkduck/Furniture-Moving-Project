package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.ProviderPackagePricingDTO;
import java.util.List;

public interface ProviderPricingService {
    List<ProviderPackagePricingDTO> listPackagesForProvider(Integer providerId);
    ProviderPackagePricingDTO getPackagePricing(Integer providerId, Integer packageId);
    void savePackagePricing(ProviderPackagePricingDTO dto);
}
