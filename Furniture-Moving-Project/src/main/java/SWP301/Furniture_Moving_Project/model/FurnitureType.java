// src/main/java/SWP301/Furniture_Moving_Project/model/FurnitureType.java
package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

@Entity @Table(name="furniture_types")
public class FurnitureType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="furniture_type_id")
    private Integer furnitureTypeId;

    @Column(name="code", nullable=false, unique=true)
    private String code;

    @Column(name="name", nullable=false)
    private String name;

    @Column(name="unit")
    private String unit;

    // getters/setters


    public Integer getFurnitureTypeId() {
        return furnitureTypeId;
    }

    public void setFurnitureTypeId(Integer furnitureTypeId) {
        this.furnitureTypeId = furnitureTypeId;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
