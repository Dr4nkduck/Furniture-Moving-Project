package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.ProviderServiceItemDTO;
import java.util.List;

public interface ProviderCatalogService {
    List<ProviderServiceItemDTO> listMyItems(Long providerId);
    ProviderServiceItemDTO create(Long providerId, ProviderServiceItemDTO request);
    ProviderServiceItemDTO update(Long providerId, Long serviceItemId, ProviderServiceItemDTO request);
    void delete(Long providerId, Long serviceItemId);
}
