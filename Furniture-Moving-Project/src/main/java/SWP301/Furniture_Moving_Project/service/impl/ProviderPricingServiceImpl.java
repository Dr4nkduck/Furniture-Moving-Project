package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.*;
import SWP301.Furniture_Moving_Project.model.*;
import SWP301.Furniture_Moving_Project.repository.*;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ProviderPricingServiceImpl implements ProviderPricingService {

    private final ProviderServicePackageRepository pspRepo;
    private final ProviderPackageFurniturePriceRepository ppfpRepo;
    private final ProviderRepository providerRepo;
    private final ServicePackageRepository packageRepo;
    private final FurnitureTypeRepository furnitureTypeRepo;

    public ProviderPricingServiceImpl(ProviderServicePackageRepository pspRepo,
                                      ProviderPackageFurniturePriceRepository ppfpRepo,
                                      ProviderRepository providerRepo,
                                      ServicePackageRepository packageRepo,
                                      FurnitureTypeRepository furnitureTypeRepo) {
        this.pspRepo = pspRepo;
        this.ppfpRepo = ppfpRepo;
        this.providerRepo = providerRepo;
        this.packageRepo = packageRepo;
        this.furnitureTypeRepo = furnitureTypeRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageOptionDTO> listPackages(Integer providerId) {
        List<ServicePackage> all = packageRepo.findAll();
        Map<Integer, BigDecimal> perKmByPackage = new HashMap<>();
        for (var row : pspRepo.findByProvider_ProviderId(providerId)) {
            if (row.getPerKm() != null) {
                perKmByPackage.put(row.getServicePackage().getPackageId(), row.getPerKm());
            }
        }

        List<PackageOptionDTO> out = new ArrayList<>();
        for (ServicePackage sp : all) {
            PackageOptionDTO dto = new PackageOptionDTO();
            dto.packageId = sp.getPackageId();
            dto.packageName = sp.getName();
            dto.pricePerKm = perKmByPackage.get(sp.getPackageId());
            out.add(dto);
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public PackagePricingDetailDTO getPackageDetail(Integer providerId, Integer packageId) {
        PackagePricingDetailDTO dto = new PackagePricingDetailDTO();
        dto.pricePerKm = pspRepo.findByProvider_ProviderIdAndServicePackage_PackageId(providerId, packageId)
                .map(ProviderServicePackage::getPerKm).orElse(null);

        var rows = ppfpRepo.findByProvider_ProviderIdAndServicePackage_PackageId(providerId, packageId);
        List<FurniturePriceDTO> prices = new ArrayList<>();
        for (var r : rows) {
            FurniturePriceDTO f = new FurniturePriceDTO();
            f.furnitureItemId = r.getFurnitureType().getFurnitureTypeId();
            f.furnitureItemName = r.getFurnitureType().getName();
            f.price = r.getPrice();
            prices.add(f);
        }
        dto.furniturePrices = prices;
        return dto;
    }

    @Override
    @Transactional
    public void savePackagePricing(PricingSaveRequestDTO req) {
        Provider provider = providerRepo.findById(req.providerId)
                .orElseThrow(() -> new NoSuchElementException("Provider not found"));
        ServicePackage sp = packageRepo.findById(req.packageId)
                .orElseThrow(() -> new NoSuchElementException("Package not found"));

        // upsert per_km
        ProviderServicePackage psp = pspRepo.findByProvider_ProviderIdAndServicePackage_PackageId(req.providerId, req.packageId)
                .orElseGet(() -> {
                    ProviderServicePackage n = new ProviderServicePackage();
                    n.setProvider(provider);
                    n.setServicePackage(sp);
                    return n;
                });
        psp.setPerKm(req.pricePerKm);
        pspRepo.save(psp);

        // upsert bảng giá nội thất theo gói
        if (req.furniturePrices != null) {
            for (var ip : req.furniturePrices) {
                if (ip.price == null) {
                    // xoá nếu có
                    if (ip.furnitureItemId != null) {
                        ppfpRepo.findByProvider_ProviderIdAndServicePackage_PackageIdAndFurnitureType_FurnitureTypeId(
                                req.providerId, req.packageId, ip.furnitureItemId
                        ).ifPresent(ppfpRepo::delete);
                    }
                    continue;
                }

                FurnitureType ft;
                if (ip.furnitureItemId != null) {
                    ft = furnitureTypeRepo.findById(ip.furnitureItemId)
                            .orElseThrow(() -> new NoSuchElementException("FurnitureType not found: " + ip.furnitureItemId));
                } else {
                    // cho phép thêm mới theo tên
                    ft = furnitureTypeRepo.findByName(ip.furnitureItemName)
                            .orElseGet(() -> {
                                FurnitureType n = new FurnitureType();
                                n.setName(ip.furnitureItemName);
                                n.setCode(UUID.randomUUID().toString().substring(0,8).toUpperCase());
                                return furnitureTypeRepo.save(n);
                            });
                }

                var row = ppfpRepo.findByProvider_ProviderIdAndServicePackage_PackageIdAndFurnitureType_FurnitureTypeId(
                        req.providerId, req.packageId, ft.getFurnitureTypeId()
                ).orElseGet(() -> {
                    ProviderPackageFurniturePrice n = new ProviderPackageFurniturePrice();
                    n.setProvider(provider);
                    n.setServicePackage(sp);
                    n.setFurnitureType(ft);
                    return n;
                });

                row.setPrice(ip.price);
                ppfpRepo.save(row);
            }
        }
    }
}
