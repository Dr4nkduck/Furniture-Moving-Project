package SWP301.Furniture_Moving_Project.dto;

public class ServicePackageListItemDTO {

    private Integer packageId;
    /** Tên hiển thị ở list bên trái: snapshot nếu có, không thì tên gốc */
    private String packageName;
    /** Tên gói gốc từ bảng service_packages (dùng cho filter) */
    private String basePackageName;
    /** Giá/km provider đã cấu hình (có thể null) */
    private Double pricePerKm;

    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(Integer packageId) {
        this.packageId = packageId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getBasePackageName() {
        return basePackageName;
    }

    public void setBasePackageName(String basePackageName) {
        this.basePackageName = basePackageName;
    }

    public Double getPricePerKm() {
        return pricePerKm;
    }

    public void setPricePerKm(Double pricePerKm) {
        this.pricePerKm = pricePerKm;
    }
}
