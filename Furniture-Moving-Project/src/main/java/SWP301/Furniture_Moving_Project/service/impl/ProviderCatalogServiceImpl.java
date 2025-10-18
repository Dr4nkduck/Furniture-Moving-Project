package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.ProviderServiceItemDTO;
import SWP301.Furniture_Moving_Project.model.Provider;
import SWP301.Furniture_Moving_Project.model.ProviderServiceItem;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ProviderServiceItemRepository;
import SWP301.Furniture_Moving_Project.service.ProviderCatalogService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProviderCatalogServiceImpl implements ProviderCatalogService {

    private final ProviderServiceItemRepository providerServiceItemRepository;
    private final ProviderRepository providerRepository;

    public ProviderCatalogServiceImpl(ProviderServiceItemRepository providerServiceItemRepository,
                                      ProviderRepository providerRepository) {
        this.providerServiceItemRepository = providerServiceItemRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    public List<ProviderServiceItemDTO> listMyItems(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found"));

        return providerServiceItemRepository.findByProviderAndActiveTrueOrderByNameAsc(provider)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProviderServiceItemDTO create(Long providerId, ProviderServiceItemDTO request) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found"));

        ProviderServiceItem entity = new ProviderServiceItem();
        entity.setProvider(provider);
        apply(entity, request);
        entity.setActive(true);

        return toDto(providerServiceItemRepository.save(entity));
    }

    @Override
    public ProviderServiceItemDTO update(Long providerId, Long serviceItemId, ProviderServiceItemDTO request) {
        ProviderServiceItem entity = providerServiceItemRepository.findById(serviceItemId)
                .orElseThrow(() -> new EntityNotFoundException("Service item not found"));

        if (entity.getProvider() == null || !entity.getProvider().getProviderId().equals(providerId)) {
            throw new EntityNotFoundException("Service item not owned by this provider");
        }

        apply(entity, request);
        return toDto(providerServiceItemRepository.save(entity));
    }

    @Override
    public void delete(Long providerId, Long serviceItemId) {
        ProviderServiceItem entity = providerServiceItemRepository.findById(serviceItemId)
                .orElseThrow(() -> new EntityNotFoundException("Service item not found"));

        if (entity.getProvider() == null || !entity.getProvider().getProviderId().equals(providerId)) {
            throw new EntityNotFoundException("Service item not owned by this provider");
        }

        entity.setActive(false); // soft-delete
        providerServiceItemRepository.save(entity);
    }

    /* -------------------- helpers -------------------- */

    private ProviderServiceItemDTO toDto(ProviderServiceItem entity) {
        ProviderServiceItemDTO dto = new ProviderServiceItemDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setBaseFee(entity.getBaseFee());
        dto.setPerKm(entity.getPerKm());
        dto.setPerMin(entity.getPerMin());
        dto.setSurchargeStairs(entity.getSurchargeStairs());
        dto.setSurchargeNoElevator(entity.getSurchargeNoElevator());
        dto.setSurchargeNarrowAlley(entity.getSurchargeNarrowAlley());
        dto.setSurchargeWeekend(entity.getSurchargeWeekend());
        dto.setActive(entity.isActive());
        return dto;
    }

    private void apply(ProviderServiceItem entity, ProviderServiceItemDTO dto) {
        entity.setName(dto.getName());
        entity.setBaseFee(dto.getBaseFee());
        entity.setPerKm(dto.getPerKm());
        entity.setPerMin(dto.getPerMin());
        entity.setSurchargeStairs(dto.getSurchargeStairs());
        entity.setSurchargeNoElevator(dto.getSurchargeNoElevator());
        entity.setSurchargeNarrowAlley(dto.getSurchargeNarrowAlley());
        entity.setSurchargeWeekend(dto.getSurchargeWeekend());
    }
}
