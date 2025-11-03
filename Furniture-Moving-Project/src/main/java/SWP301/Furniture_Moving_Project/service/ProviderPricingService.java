package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.*;

import java.util.List;

public interface ProviderPricingService {

    List<PackageOptionDTO> listPackages(Integer providerId);

    PackagePricingDetailDTO getPackageDetail(Integer providerId, Integer packageId);

    void savePackagePricing(PricingSaveRequestDTO req);
}
