package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.ProviderPricingDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderPricingServiceImpl implements ProviderPricingService {
    private final ProviderRepository providerRepository;

    public ProviderPricingServiceImpl(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderPricingDTO getPricing(Integer providerId) {
        Provider p = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        ProviderPricingDTO dto = new ProviderPricingDTO();
        dto.setBaseFee(p.getBaseFee());
        dto.setPerKm(p.getPerKm());
        dto.setPerMinute(p.getPerMinute());
        dto.setSurchargeStairs(p.getSurchargeStairs());
        dto.setSurchargeNoElevator(p.getSurchargeNoElevator());
        dto.setSurchargeNarrowAlley(p.getSurchargeNarrowAlley());
        dto.setSurchargeWeekend(p.getSurchargeWeekend());
        return dto;
    }

    @Override
    @Transactional
    public ProviderPricingDTO upsertPricing(Integer providerId, ProviderPricingDTO dto) {
        Provider p = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        p.setBaseFee(dto.getBaseFee());
        p.setPerKm(dto.getPerKm());
        p.setPerMinute(dto.getPerMinute());
        p.setSurchargeStairs(dto.getSurchargeStairs());
        p.setSurchargeNoElevator(dto.getSurchargeNoElevator());
        p.setSurchargeNarrowAlley(dto.getSurchargeNarrowAlley());
        p.setSurchargeWeekend(dto.getSurchargeWeekend());
        providerRepository.save(p);
        return dto;
    }
}
