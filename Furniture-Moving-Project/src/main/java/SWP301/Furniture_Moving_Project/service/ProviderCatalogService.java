package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.FurniturePriceDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPackagePricingDTO;
import SWP301.Furniture_Moving_Project.model.ServicePackage;

import java.util.List;

public interface ProviderCatalogService {
    List<ServicePackage> listActivePackages();

    ProviderPackagePricingDTO getPackagePricing(Integer providerId, Integer packageId);

    ProviderPackagePricingDTO upsertPackagePricing(Integer providerId, ProviderPackagePricingDTO dto);

    List<FurniturePriceDTO> listFurniturePrices(Integer providerId);

    List<FurniturePriceDTO> upsertFurniturePrices(Integer providerId, List<FurniturePriceDTO> items);
}
