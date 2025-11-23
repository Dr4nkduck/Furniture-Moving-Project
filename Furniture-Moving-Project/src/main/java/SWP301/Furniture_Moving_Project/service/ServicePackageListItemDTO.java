package SWP301.Furniture_Moving_Project.service;

public class ServicePackageListItemDTO {

    // id gói trong bảng service_packages
    private Integer packageId;

    // tên gốc trong bảng service_packages
    private String basePackageName;

    // tên hiển thị cho provider (snapshot), nếu null thì dùng basePackageName
    private String packageName;

    // giá/km mà provider đã set (có thể null nếu chưa cấu hình)
    private Double pricePerKm;

    // ===== getters/setters =====
    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(Integer packageId) {
        this.packageId = packageId;
    }

    public String getBasePackageName() {
        return basePackageName;
    }

    public void setBasePackageName(String basePackageName) {
        this.basePackageName = basePackageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Double getPricePerKm() {
        return pricePerKm;
    }

    public void setPricePerKm(Double pricePerKm) {
        this.pricePerKm = pricePerKm;
    }
}
