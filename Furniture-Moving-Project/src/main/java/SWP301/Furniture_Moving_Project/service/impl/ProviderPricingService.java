package SWP301.Furniture_Moving_Project.service.impl;

import SWP301.Furniture_Moving_Project.dto.ProviderPackageSnapshotDTO;
import SWP301.Furniture_Moving_Project.dto.ServicePackageListItemDTO;

import java.util.List;

public interface ProviderPricingService {

    // LEFT LIST: tất cả gói + giá/km (nếu đã cấu hình)
    List<ServicePackageListItemDTO> listPackages(Integer providerId);

    // RIGHT DETAIL: snapshot + bảng giá nội thất
    ProviderPackageSnapshotDTO getPackage(Integer providerId, Integer packageId);

    // Lưu snapshot (tên + giá/km + bảng giá)
    void saveSnapshot(Integer providerId, Integer packageId, ProviderPackageSnapshotDTO body);

    // Xoá toàn bộ snapshot (xem như reset)
    void clearSnapshot(Integer providerId, Integer packageId);

    // Xoá 1 dòng nội thất
    void deleteItem(Integer providerId, Integer packageId, Integer furnitureTypeId);
}
