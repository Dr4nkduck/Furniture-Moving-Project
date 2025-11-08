// service/ProviderPricingService.java
package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.*;

import java.util.List;

public interface ProviderPricingService {
    List<ServicePackageListItemDTO> listPackages(Integer providerId);
    ProviderPackageSnapshotDTO getPackage(Integer providerId, Integer packageId);
    void saveSnapshot(Integer providerId, Integer packageId, ProviderPackageSnapshotDTO body);
    void clearSnapshot(Integer providerId, Integer packageId);
    void deleteItem(Integer providerId, Integer packageId, Integer furnitureTypeId);
}
