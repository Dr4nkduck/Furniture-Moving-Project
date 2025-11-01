package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.FurniturePriceDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPackagePricingDTO;
import SWP301.Furniture_Moving_Project.model.*;
import SWP301.Furniture_Moving_Project.repository.*;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProviderPricingServiceImpl implements ProviderPricingService {

    private final ProviderRepository providerRepo;
    private final ServicePackageRepository packageRepo;
    private final FurnitureItemRepository furnitureRepo;
    private final ProviderServicePackageRepository pspRepo;
    private final ProviderFurniturePriceRepository pfpRepo;

    public ProviderPricingServiceImpl(ProviderRepository providerRepo,
                                      ServicePackageRepository packageRepo,
                                      FurnitureItemRepository furnitureRepo,
                                      ProviderServicePackageRepository pspRepo,
                                      ProviderFurniturePriceRepository pfpRepo) {
        this.providerRepo = providerRepo;
        this.packageRepo = packageRepo;
        this.furnitureRepo = furnitureRepo;
        this.pspRepo = pspRepo;
        this.pfpRepo = pfpRepo;
    }

    @Override
    public List<ProviderPackagePricingDTO> listPackagesForProvider(Integer providerId) {
        Provider provider = providerRepo.findById(providerId)
                .orElseThrow(() -> new NoSuchElementException("Provider not found: " + providerId));

        List<ServicePackage> all = packageRepo.findAll();
        Map<Integer, BigDecimal> perKm = pspRepo.findByProvider(provider).stream()
                .collect(Collectors.toMap(psp -> psp.getServicePackage().getPackageId(),
                        ProviderServicePackage::getPricePerKm));

        List<ProviderPackagePricingDTO> out = new ArrayList<>();
        for (ServicePackage sp : all) {
            ProviderPackagePricingDTO dto = new ProviderPackagePricingDTO();
            dto.setProviderId(providerId);
            dto.setPackageId(sp.getPackageId());
            dto.setPackageName(sp.getName());
            dto.setPricePerKm(perKm.get(sp.getPackageId())); // null nếu chưa set
            out.add(dto);
        }
        return out;
    }

    @Override
    public ProviderPackagePricingDTO getPackagePricing(Integer providerId, Integer packageId) {
        Provider provider = providerRepo.findById(providerId)
                .orElseThrow(() -> new NoSuchElementException("Provider not found: " + providerId));
        ServicePackage sp = packageRepo.findById(packageId)
                .orElseThrow(() -> new NoSuchElementException("ServicePackage not found: " + packageId));

        BigDecimal pricePerKm = pspRepo.findByProviderAndServicePackage(provider, sp)
                .map(ProviderServicePackage::getPricePerKm).orElse(null);

        Map<Integer, ProviderFurniturePrice> priceMap = pfpRepo
                .findByProviderAndServicePackage(provider, sp)
                .stream().collect(Collectors.toMap(p -> p.getFurnitureItem().getItemId(), p -> p));

        List<FurniturePriceDTO> rows = new ArrayList<>();
        for (FurnitureItem fi : furnitureRepo.findAll()) {
            FurniturePriceDTO d = new FurniturePriceDTO();
            d.setFurnitureItemId(fi.getItemId());
            d.setFurnitureItemName(fi.getItemType());
            ProviderFurniturePrice p = priceMap.get(fi.getItemId());
            d.setPrice(p != null ? p.getPrice() : null);
            rows.add(d);
        }

        ProviderPackagePricingDTO dto = new ProviderPackagePricingDTO();
        dto.setProviderId(providerId);
        dto.setPackageId(packageId);
        dto.setPackageName(sp.getName());
        dto.setPricePerKm(pricePerKm);
        dto.setFurniturePrices(rows);
        return dto;
    }

    @Transactional
    @Override
    public void savePackagePricing(ProviderPackagePricingDTO dto) {
        Integer providerId = dto.getProviderId();
        Integer packageId  = dto.getPackageId();

        Provider provider = providerRepo.findById(providerId)
                .orElseThrow(() -> new NoSuchElementException("Provider not found: " + providerId));
        ServicePackage sp = packageRepo.findById(packageId)
                .orElseThrow(() -> new NoSuchElementException("ServicePackage not found: " + packageId));

        // Upsert price/km
        ProviderServicePackage psp = pspRepo.findByProviderAndServicePackage(provider, sp)
                .orElseGet(() -> {
                    ProviderServicePackage x = new ProviderServicePackage();
                    x.setProvider(provider);
                    x.setServicePackage(sp);
                    return x;
                });
        psp.setPricePerKm(dto.getPricePerKm());
        pspRepo.save(psp);

        // Upsert/Xoá giá món đồ
        if (dto.getFurniturePrices() != null) {
            for (var fp : dto.getFurniturePrices()) {
                FurnitureItem fi = furnitureRepo.findById(fp.getFurnitureItemId())
                        .orElseThrow(() -> new NoSuchElementException("FurnitureItem not found: " + fp.getFurnitureItemId()));

                if (fp.getPrice() == null) {
                    pfpRepo.findByProviderAndServicePackageAndFurnitureItem(provider, sp, fi)
                            .ifPresent(e -> pfpRepo.deleteByProviderAndServicePackageAndFurnitureItem(provider, sp, fi));
                } else {
                    ProviderFurniturePrice row = pfpRepo
                            .findByProviderAndServicePackageAndFurnitureItem(provider, sp, fi)
                            .orElseGet(() -> {
                                ProviderFurniturePrice r = new ProviderFurniturePrice();
                                r.setProvider(provider);
                                r.setServicePackage(sp);
                                r.setFurnitureItem(fi);
                                return r;
                            });
                    row.setPrice(fp.getPrice());
                    pfpRepo.save(row);
                }
            }
        }
    }
}
