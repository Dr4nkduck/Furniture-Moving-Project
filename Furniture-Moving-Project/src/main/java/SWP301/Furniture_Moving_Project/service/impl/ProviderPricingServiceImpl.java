// service/impl/ProviderPricingServiceImpl.java
package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.*;
import SWP301.Furniture_Moving_Project.model.*;
import SWP301.Furniture_Moving_Project.repository.*;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProviderPricingServiceImpl implements ProviderPricingService {

    private final ServicePackageRepository spRepo;
    private final ProviderServicePackageRepository pspRepo;
    private final FurnitureTypeRepository ftRepo;
    private final ProviderPackageFurniturePriceRepository priceRepo;

    public ProviderPricingServiceImpl(ServicePackageRepository spRepo,
                                      ProviderServicePackageRepository pspRepo,
                                      FurnitureTypeRepository ftRepo,
                                      ProviderPackageFurniturePriceRepository priceRepo) {
        this.spRepo = spRepo;
        this.pspRepo = pspRepo;
        this.ftRepo = ftRepo;
        this.priceRepo = priceRepo;
    }

    @Override
    public List<ServicePackageListItemDTO> listPackages(Integer providerId) {
        // Lấy tất cả gói active + provider snapshot (nếu có)
        List<ServicePackage> all = spRepo.findByIsActiveTrueOrderByNameAsc();
        Map<Integer, ProviderServicePackage> map = pspRepo.findAllByProviderId(providerId).stream()
                .collect(Collectors.toMap(ProviderServicePackage::getPackageId, x -> x));

        List<ServicePackageListItemDTO> out = new ArrayList<>();
        for (ServicePackage sp : all) {
            ProviderServicePackage snap = map.get(sp.getPackageId());
            ServicePackageListItemDTO dto = new ServicePackageListItemDTO();
            dto.packageId = sp.getPackageId();
            dto.basePackageName = sp.getName();
            dto.packageName = (snap != null && snap.getPackageNameSnapshot() != null && !snap.getPackageNameSnapshot().isBlank())
                    ? snap.getPackageNameSnapshot() : sp.getName();
            dto.pricePerKm = (snap != null) ? snap.getPerKm() : null;
            out.add(dto);
        }
        return out;
    }

    @Override
    public ProviderPackageSnapshotDTO getPackage(Integer providerId, Integer packageId) {
        ServicePackage sp = spRepo.findById(packageId).orElseThrow(() -> new NoSuchElementException("Package not found"));
        ProviderServicePackage snap = pspRepo.findByProviderIdAndPackageId(providerId, packageId).orElse(null);
        List<ProviderPackageFurniturePrice> rows = priceRepo.findByProviderAndPackage(providerId, packageId);

        ProviderPackageSnapshotDTO dto = new ProviderPackageSnapshotDTO();
        dto.packageNameSnapshot = (snap != null && snap.getPackageNameSnapshot() != null) ? snap.getPackageNameSnapshot() : sp.getName();
        dto.pricePerKm = Double.valueOf((snap != null) ? snap.getPerKm() : null);
        dto.furniturePrices = rows.stream().map(r -> {
            FurniturePriceDTO x = new FurniturePriceDTO();
            x.furnitureItemId = r.getFurnitureTypeId();
            x.furnitureItemName = (r.getFurnitureType() != null ? r.getFurnitureType().getName() : null);
            x.price = Double.valueOf(r.getPrice());
            return x;
        }).collect(Collectors.toList());
        return dto;
    }

    @Override
    public void saveSnapshot(Integer providerId, Integer packageId, ProviderPackageSnapshotDTO body) {
        ProviderServicePackage snap = pspRepo.findByProviderIdAndPackageId(providerId, packageId).orElseGet(() -> {
            ProviderServicePackage n = new ProviderServicePackage();
            n.setProviderId(providerId);
            n.setPackageId(packageId);
            return pspRepo.save(n);
        });
        snap.setPerKm(body.pricePerKm);
        snap.setPackageNameSnapshot(emptyToNull(body.packageNameSnapshot));

        // Upsert từng item
        if (body.furniturePrices != null) {
            for (FurniturePriceDTO it : body.furniturePrices) {
                Integer fid = it.furnitureItemId;
                if (fid == null) {
                    // tạo mới FurnitureType nếu có tên
                    String name = safeTrim(it.furnitureItemName);
                    if (name == null) continue;
                    fid = ftRepo.findByNameIgnoreCase(name)
                            .map(FurnitureType::getFurnitureTypeId)
                            .orElseGet(() -> {
                                FurnitureType ft = new FurnitureType();
                                ft.setName(name);
                                ft.setCode(genCode(name));
                                return ftRepo.save(ft).getFurnitureTypeId();
                            });
                }
                if (it.price == null) {
                    // null price => xoá dòng nếu có
                    priceRepo.deleteOne(providerId, packageId, fid);
                } else {
                    ProviderPackageFurniturePrice row = priceRepo.findOne(providerId, packageId, fid);
                    if (row == null) {
                        row = new ProviderPackageFurniturePrice();
                        row.setProviderId(providerId);
                        row.setPackageId(packageId);
                        row.setFurnitureTypeId(fid);
                    }
                    row.setPrice(it.price);
                    priceRepo.save(row);
                }
            }
        }
    }

    @Override
    public void clearSnapshot(Integer providerId, Integer packageId) {
        // Xoá tất cả item + đặt null per_km & snapshot name
        priceRepo.deleteAllByProviderAndPackage(providerId, packageId);
        pspRepo.findByProviderIdAndPackageId(providerId, packageId)
                .ifPresent(s -> { s.setPerKm(null); s.setPackageNameSnapshot(null); });
    }

    @Override
    public void deleteItem(Integer providerId, Integer packageId, Integer furnitureTypeId) {
        priceRepo.deleteOne(providerId, packageId, furnitureTypeId);
    }

    private static String emptyToNull(String s){ return (s==null || s.trim().isEmpty())? null : s.trim(); }
    private static String safeTrim(String s){ return (s==null)? null : s.trim().isEmpty()? null : s.trim(); }

    private static String genCode(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase();
        if (base.isEmpty()) base = "ITEM";
        return (base.length() > 40) ? base.substring(0, 40) : base;
    }
}
