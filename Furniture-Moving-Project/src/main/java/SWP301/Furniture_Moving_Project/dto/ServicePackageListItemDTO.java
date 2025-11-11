// dto/ServicePackageListItemDTO.java
package SWP301.Furniture_Moving_Project.dto;

public class ServicePackageListItemDTO {
    public Integer packageId;
    public String  packageName;      // tên snapshot nếu có, ngược lại tên gốc
    public String  basePackageName;  // tên gốc
    public Double  pricePerKm;       // null nếu chưa cấu hình
}
