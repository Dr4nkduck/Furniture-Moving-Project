package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;

@Entity
@Table(name = "furniture_types")
public class FurnitureType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "furniture_type_id")
    private Integer furnitureTypeId;

    @Column(unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    // getters/setters
    public Integer getFurnitureTypeId() { return furnitureTypeId; }
    public void setFurnitureTypeId(Integer furnitureTypeId) { this.furnitureTypeId = furnitureTypeId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
