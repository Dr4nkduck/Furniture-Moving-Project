// src/main/java/SWP301/Furniture_Moving_Project/model/ServicePackage.java
package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

@Entity @Table(name="service_packages")
public class ServicePackage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="package_id")
    private Integer packageId;

    @Column(name="code", nullable=false, unique=true)
    private String code;

    @Column(name="name", nullable=false)
    private String name;

    @Column(name="is_active", nullable=false)
    private boolean isActive = true;

    // getters/setters


    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(Integer packageId) {
        this.packageId = packageId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
