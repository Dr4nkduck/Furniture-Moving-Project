package SWP301.Furniture_Moving_Project.dto;

import java.util.List;

public class ProviderPackageSnapshotDTO {

    // tên snapshot hiển thị cho provider
    private String packageNameSnapshot;

    // giá / km của provider
    private Double pricePerKm;

    // bảng giá nội thất
    private List<FurniturePriceDTO> furniturePrices;

    // ===== getters / setters =====
    public String getPackageNameSnapshot() {
        return packageNameSnapshot;
    }

    public void setPackageNameSnapshot(String packageNameSnapshot) {
        this.packageNameSnapshot = packageNameSnapshot;
    }

    public Double getPricePerKm() {
        return pricePerKm;
    }

    public void setPricePerKm(Double pricePerKm) {
        this.pricePerKm = pricePerKm;
    }

    public List<FurniturePriceDTO> getFurniturePrices() {
        return furniturePrices;
    }

    public void setFurniturePrices(List<FurniturePriceDTO> furniturePrices) {
        this.furniturePrices = furniturePrices;
    }
}
