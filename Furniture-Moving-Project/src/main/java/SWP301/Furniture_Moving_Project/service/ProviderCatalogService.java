package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.ProviderServiceItemDTO;
import java.util.List;

public interface ProviderCatalogService {
    List<ProviderServiceItemDTO> listMyItems(Long providerId);
    ProviderServiceItemDTO create(Long providerId, ProviderServiceItemDTO dto);
    ProviderServiceItemDTO update(Long providerId, Long itemId, ProviderServiceItemDTO dto);
    void delete(Long providerId, Long itemId); // soft delete -> active=false
}
