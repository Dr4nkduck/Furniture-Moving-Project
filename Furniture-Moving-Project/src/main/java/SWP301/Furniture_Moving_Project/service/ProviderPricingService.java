package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.ProviderPricingDTO;

public interface ProviderPricingService {
    ProviderPricingDTO getPricing(Integer providerId);
    ProviderPricingDTO upsertPricing(Integer providerId, ProviderPricingDTO dto);
}
