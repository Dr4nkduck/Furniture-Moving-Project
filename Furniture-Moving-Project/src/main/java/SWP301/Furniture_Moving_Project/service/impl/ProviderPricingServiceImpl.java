// service/impl/ProviderPricingServiceImpl.java
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

    @Transactional(readOnly = true)
    public List<PackageOptionDTO> listPackages(Integer providerId) {
        var all = packageRepo.findAll();
        var mine = pspRepo.findByProvider_ProviderId(providerId);
        Map<Integer, BigDecimal> kmByPkg = new HashMap<>();
        for (var r : mine) {
            if (r.getPricePerKm() != null)
                kmByPkg.put(r.getServicePackage().getPackageId(), r.getPricePerKm());
        }

        List<PackageOptionDTO> out = new ArrayList<>();
        for (var sp : all) {
            var dto = new PackageOptionDTO();
            dto.packageId = sp.getPackageId();
            dto.packageName = sp.getName();
            dto.pricePerKm = kmByPkg.get(sp.getPackageId());
            out.add(dto);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public PackagePricingDetailDTO getPackageDetail(Integer providerId, Integer packageId) {
        var dto = new PackagePricingDetailDTO();
        var pspOpt = pspRepo.findByProvider_ProviderIdAndServicePackage_PackageId(providerId, packageId);

        dto.pricePerKm = pspOpt.map(ProviderServicePackage::getPricePerKm).orElse(null);
        dto.packageNameSnapshot = pspOpt.map(ProviderServicePackage::getPackageNameSnapshot)
                .orElseGet(() -> packageRepo.findById(packageId).map(ServicePackage::getName).orElse(null));

        var rows = ppfpRepo.findByProvider_ProviderIdAndServicePackage_PackageId(providerId, packageId);
        List<FurniturePriceDTO> items = new ArrayList<>();
        for (var r : rows) {
            var it = new FurniturePriceDTO();
            it.furnitureItemId = r.getFurnitureType().getFurnitureTypeId();
            it.furnitureItemName = r.getFurnitureType().getName();
            it.price = r.getPrice();
            items.add(it);
        }
        dto.furniturePrices = items;
        return dto;
    }

    @Transactional
    public void savePackagePricing(PricingSaveRequestDTO req) {
        var provider = providerRepo.findById(req.providerId)
                .orElseThrow(() -> new NoSuchElementException("Provider not found"));
        var sp = packageRepo.findById(req.packageId)
                .orElseThrow(() -> new NoSuchElementException("Package not found"));

        // upsert PSP (snapshot)
        var psp = pspRepo.findByProvider_ProviderIdAndServicePackage_PackageId(req.providerId, req.packageId)
                .orElseGet(() -> {
                    var n = new ProviderServicePackage();
                    n.setProvider(provider);
                    n.setServicePackage(sp);
                    return n;
                });

        psp.setPricePerKm(req.pricePerKm);
        // snapshot tên gói: ưu tiên req.packageName, nếu null lấy từ service_packages
        String snap = (req.packageName != null && !req.packageName.isBlank())
                ? req.packageName
                : sp.getName();
        psp.setPackageNameSnapshot(snap);
        pspRepo.save(psp);

        // upsert bảng giá nội thất theo gói
        if (req.furniturePrices != null) {
            for (var ip : req.furniturePrices) {
                if (ip.price == null) {
                    // nếu client gửi null -> xóa giá
                    if (ip.furnitureItemId != null) {
                        ppfpRepo.findByProvider_ProviderIdAndServicePackage_PackageIdAndFurnitureType_FurnitureTypeId(
                                req.providerId, req.packageId, ip.furnitureItemId
                        ).ifPresent(ppfpRepo::delete);
                    }
                    continue;
                }

                // xác định FurnitureType
                FurnitureType ft;
                if (ip.furnitureItemId != null) {
                    ft = furnitureTypeRepo.findById(ip.furnitureItemId)
                            .orElseThrow(() -> new NoSuchElementException("FurnitureType not found: " + ip.furnitureItemId));
                } else {
                    String name = Objects.requireNonNull(ip.furnitureItemName, "furnitureItemName required");
                    ft = furnitureTypeRepo.findByName(name).orElseGet(() -> {
                        var n = new FurnitureType();
                        n.setName(name);
                        // code tạo tạm từ tên
                        var code = name.toUpperCase().replaceAll("[^A-Z0-9]", "");
                        if (code.length() > 10) code = code.substring(0, 10);
                        if (code.isBlank()) code = "ITEM" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
                        n.setCode(code);
                        return furnitureTypeRepo.save(n);
                    });
                }

                var row = ppfpRepo.findByProvider_ProviderIdAndServicePackage_PackageIdAndFurnitureType_FurnitureTypeId(
                        req.providerId, req.packageId, ft.getFurnitureTypeId()
                ).orElseGet(() -> {
                    var n = new ProviderPackageFurniturePrice();
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
