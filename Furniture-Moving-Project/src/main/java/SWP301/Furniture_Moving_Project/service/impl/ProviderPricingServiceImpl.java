package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.FurniturePriceDTO;
import SWP301.Furniture_Moving_Project.dto.ProviderPackageSnapshotDTO;
import SWP301.Furniture_Moving_Project.dto.ServicePackageListItemDTO;
import SWP301.Furniture_Moving_Project.model.FurnitureType;
import SWP301.Furniture_Moving_Project.model.ProviderPackageFurniturePrice;
import SWP301.Furniture_Moving_Project.model.ProviderServicePackage;
import SWP301.Furniture_Moving_Project.model.ServicePackage;
import SWP301.Furniture_Moving_Project.repository.ProviderPackageFurniturePriceRepository;
import SWP301.Furniture_Moving_Project.repository.ProviderServicePackageRepository;
import SWP301.Furniture_Moving_Project.repository.ServicePackageRepository;
import SWP301.Furniture_Moving_Project.service.ProviderPricingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProviderPricingServiceImpl implements ProviderPricingService {

    private final ServicePackageRepository servicePackageRepo;
    private final ProviderServicePackageRepository providerServicePackageRepo;
    private final ProviderPackageFurniturePriceRepository providerPackageFurnitureRepo;

    public ProviderPricingServiceImpl(ServicePackageRepository servicePackageRepo,
                                      ProviderServicePackageRepository providerServicePackageRepo,
                                      ProviderPackageFurniturePriceRepository providerPackageFurnitureRepo) {
        this.servicePackageRepo = servicePackageRepo;
        this.providerServicePackageRepo = providerServicePackageRepo;
        this.providerPackageFurnitureRepo = providerPackageFurnitureRepo;
    }

    /**
     * Danh sách tất cả gói (từ service_packages) + giá/km provider nếu đã cấu hình
     */
    @Override
    public List<ServicePackageListItemDTO> listPackages(Integer providerId) {

        // tất cả gói gốc
        List<ServicePackage> bases = servicePackageRepo.findAll();

        // map snapshot của provider theo package_id
        Map<Integer, ProviderServicePackage> snapByPkgId =
                providerServicePackageRepo.findAllByProviderId(providerId)
                        .stream()
                        .collect(Collectors.toMap(
                                ProviderServicePackage::getPackageId,
                                Function.identity()
                        ));

        List<ServicePackageListItemDTO> result = new ArrayList<>();

        for (ServicePackage base : bases) {
            Integer pkgId = base.getPackageId();

            // vì class ServicePackage của bạn không có getPackageName()
            // nên mình đặt tên mặc định "Gói #id"
            String baseName = "Gói #" + pkgId;

            ProviderServicePackage snap = snapByPkgId.get(pkgId);

            ServicePackageListItemDTO dto = new ServicePackageListItemDTO();
            dto.setPackageId(pkgId);
            dto.setBasePackageName(baseName);

            // nếu provider có snapshot name thì dùng, không thì dùng baseName
            String displayName;
            if (snap != null && snap.getPackageNameSnapshot() != null
                    && !snap.getPackageNameSnapshot().isBlank()) {
                displayName = snap.getPackageNameSnapshot();
            } else {
                displayName = baseName;
            }
            dto.setPackageName(displayName);
            dto.setPricePerKm(snap != null ? snap.getPerKm() : null);

            result.add(dto);
        }

        return result;
    }

    /**
     * Chi tiết snapshot + bảng giá nội thất cho 1 gói
     */
    @Override
    @Transactional(readOnly = true)
    public ProviderPackageSnapshotDTO getPackage(Integer providerId, Integer packageId) {

        // đảm bảo gói tồn tại
        servicePackageRepo.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói #" + packageId));

        ProviderServicePackage snap = providerServicePackageRepo
                .findByProviderIdAndPackageId(providerId, packageId)
                .orElse(null);

        ProviderPackageSnapshotDTO dto = new ProviderPackageSnapshotDTO();
        dto.setPackageNameSnapshot(snap != null ? snap.getPackageNameSnapshot() : null);
        dto.setPricePerKm(snap != null ? snap.getPerKm() : null);

        // bảng giá nội thất
        List<ProviderPackageFurniturePrice> rows =
                providerPackageFurnitureRepo.findByProviderAndPackage(providerId, packageId);

        List<FurniturePriceDTO> prices = new ArrayList<>();

        for (ProviderPackageFurniturePrice row : rows) {
            FurniturePriceDTO fp = new FurniturePriceDTO();
            fp.setFurnitureTypeId(row.getFurnitureTypeId());

            // Không dùng FurnitureType.getTypeName() nữa để tránh lỗi compile,
            // tạm thời hiển thị "Mã: <id>". Sau này nếu bạn có field name trong FurnitureType
            // thì chỉ cần sửa dòng dưới.
            FurnitureType type = row.getFurnitureType();
            String name = (type != null)
                    ? ("Mã: " + row.getFurnitureTypeId())
                    : ("Mã: " + row.getFurnitureTypeId());
            fp.setFurnitureTypeName(name);

            fp.setPrice(row.getPrice());
            prices.add(fp);
        }

        dto.setFurniturePrices(prices);
        return dto;
    }

    /**
     * Lưu snapshot: tên gói + giá/km + bảng giá nội thất
     */
    @Override
    public void saveSnapshot(Integer providerId, Integer packageId, ProviderPackageSnapshotDTO body) {

        // 1) header snapshot
        ProviderServicePackage snap = providerServicePackageRepo
                .findByProviderIdAndPackageId(providerId, packageId)
                .orElseGet(() -> {
                    ProviderServicePackage p = new ProviderServicePackage();
                    p.setProviderId(providerId);
                    p.setPackageId(packageId);
                    return p;
                });

        snap.setPerKm(body.getPricePerKm());

        String name = body.getPackageNameSnapshot();
        if (name != null && !name.isBlank()) {
            snap.setPackageNameSnapshot(name.trim());
        } else {
            snap.setPackageNameSnapshot(null);
        }

        providerServicePackageRepo.save(snap);

        // 2) bảng giá nội thất
        providerPackageFurnitureRepo.deleteAllByProviderAndPackage(providerId, packageId);

        if (body.getFurniturePrices() != null) {
            for (FurniturePriceDTO fp : body.getFurniturePrices()) {
                if (fp.getFurnitureTypeId() == null) continue; // bỏ dòng rỗng

                ProviderPackageFurniturePrice row = new ProviderPackageFurniturePrice();
                row.setProviderId(providerId);
                row.setPackageId(packageId);
                row.setFurnitureTypeId(fp.getFurnitureTypeId());
                row.setPrice(fp.getPrice() != null ? fp.getPrice() : 0d);

                providerPackageFurnitureRepo.save(row);
            }
        }
    }

    /**
     * Xoá toàn bộ snapshot (header + items)
     */
    @Override
    public void clearSnapshot(Integer providerId, Integer packageId) {
        providerPackageFurnitureRepo.deleteAllByProviderAndPackage(providerId, packageId);
        providerServicePackageRepo.deleteByProviderIdAndPackageId(providerId, packageId);
    }

    /**
     * Xoá 1 dòng nội thất
     */
    @Override
    public void deleteItem(Integer providerId, Integer packageId, Integer furnitureTypeId) {
        providerPackageFurnitureRepo.deleteOne(providerId, packageId, furnitureTypeId);
    }
}
