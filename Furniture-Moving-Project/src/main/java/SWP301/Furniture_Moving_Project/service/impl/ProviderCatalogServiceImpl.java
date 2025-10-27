package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.FurniturePriceDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPackagePricingDTO;
import SWP301.Furniture_Moving_Project.model.*;
import SWP301.Furniture_Moving_Project.repository.*;
import SWP301.Furniture_Moving_Project.service.ProviderCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProviderCatalogServiceImpl implements ProviderCatalogService {

    private final ServicePackageRepository spRepo;
    private final ProviderServicePackageRepository pspRepo;
    private final FurnitureTypeRepository ftRepo;
    private final ProviderFurniturePriceRepository pfpRepo;

    public ProviderCatalogServiceImpl(ServicePackageRepository spRepo,
                                      ProviderServicePackageRepository pspRepo,
                                      FurnitureTypeRepository ftRepo,
                                      ProviderFurniturePriceRepository pfpRepo) {
        this.spRepo = spRepo;
        this.pspRepo = pspRepo;
        this.ftRepo = ftRepo;
        this.pfpRepo = pfpRepo;
    }

    @Override
    public List<ServicePackage> listActivePackages() {
        return spRepo.findByActiveTrueOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderPackagePricingDTO getPackagePricing(Integer providerId, Integer packageId) {
        ProviderServicePackage psp = pspRepo.findByProviderIdAndServicePackage_PackageId(providerId, packageId)
                .orElseGet(() -> {
                    ProviderServicePackage x = new ProviderServicePackage();
                    x.setProviderId(providerId);
                    x.setServicePackage(spRepo.findById(packageId).orElseThrow());
                    return x;
                });
        ProviderPackagePricingDTO dto = new ProviderPackagePricingDTO();
        dto.setPackageId(psp.getServicePackage().getPackageId());
        dto.setPackageName(psp.getServicePackage().getName());
        dto.setBaseFee(psp.getBaseFee());
        dto.setPerKm(psp.getPerKm());
        dto.setPerMinute(psp.getPerMinute());
        dto.setSurchargeStairs(psp.getSurchargeStairs());
        dto.setSurchargeNoElevator(psp.getSurchargeNoElevator());
        dto.setSurchargeNarrowAlley(psp.getSurchargeNarrowAlley());
        dto.setSurchargeWeekend(psp.getSurchargeWeekend());
        return dto;
    }

    @Override
    @Transactional
    public ProviderPackagePricingDTO upsertPackagePricing(Integer providerId, ProviderPackagePricingDTO dto) {
        ServicePackage sp = spRepo.findById(dto.getPackageId()).orElseThrow();
        ProviderServicePackage psp = pspRepo
                .findByProviderIdAndServicePackage_PackageId(providerId, sp.getPackageId())
                .orElseGet(() -> {
                    ProviderServicePackage x = new ProviderServicePackage();
                    x.setProviderId(providerId);
                    x.setServicePackage(sp);
                    return x;
                });

        psp.setBaseFee(dto.getBaseFee());
        psp.setPerKm(dto.getPerKm());
        psp.setPerMinute(dto.getPerMinute());
        psp.setSurchargeStairs(dto.getSurchargeStairs());
        psp.setSurchargeNoElevator(dto.getSurchargeNoElevator());
        psp.setSurchargeNarrowAlley(dto.getSurchargeNarrowAlley());
        psp.setSurchargeWeekend(dto.getSurchargeWeekend());
        pspRepo.save(psp);

        dto.setPackageName(sp.getName());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FurniturePriceDTO> listFurniturePrices(Integer providerId) {
        List<FurnitureType> types = ftRepo.findAllByOrderByNameAsc();
        Map<Integer, ProviderFurniturePrice> current = new HashMap<>();
        for (ProviderFurniturePrice p : pfpRepo.findByProviderId(providerId)) {
            current.put(p.getFurnitureType().getFurnitureTypeId(), p);
        }
        List<FurniturePriceDTO> out = new ArrayList<>();
        for (FurnitureType t : types) {
            FurniturePriceDTO dto = new FurniturePriceDTO();
            dto.setFurnitureTypeId(t.getFurnitureTypeId());
            dto.setFurnitureName(t.getName());
            dto.setUnit(t.getUnit());
            ProviderFurniturePrice row = current.get(t.getFurnitureTypeId());
            dto.setPrice(row != null ? row.getPrice() : java.math.BigDecimal.ZERO);
            out.add(dto);
        }
        return out;
    }

    @Override
    @Transactional
    public List<FurniturePriceDTO> upsertFurniturePrices(Integer providerId, List<FurniturePriceDTO> items) {
        for (FurniturePriceDTO it : items) {
            FurnitureType type = ftRepo.findById(it.getFurnitureTypeId()).orElseThrow();
            ProviderFurniturePrice row = pfpRepo
                    .findByProviderIdAndFurnitureType_FurnitureTypeId(providerId, type.getFurnitureTypeId())
                    .orElseGet(() -> {
                        ProviderFurniturePrice x = new ProviderFurniturePrice();
                        x.setProviderId(providerId);
                        x.setFurnitureType(type);
                        return x;
                    });
            row.setPrice(it.getPrice());
            pfpRepo.save(row);
        }
        return listFurniturePrices(providerId);
    }
}
