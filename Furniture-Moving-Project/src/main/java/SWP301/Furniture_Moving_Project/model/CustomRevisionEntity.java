// src/main/java/SWP301/Furniture_Moving_Project/model/CustomRevisionEntity.java
package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import java.util.Date;

/**
 * Revision entity dùng cho Hibernate Envers.
 * - Kế thừa DefaultRevisionEntity (có rev, timestamp)
 * - Thêm trường modifiedBy và helper getRevisionDate()
 */
@Entity
@Table(name = "revinfo") // bảng revision
@RevisionEntity(CustomRevisionListener.class)
public class CustomRevisionEntity extends DefaultRevisionEntity {

    @Column(name = "modified_by")
    private String modifiedBy; // có thể là username hoặc userId tùy bạn

    public String getModifiedBy() {
        return modifiedBy;
    }
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    /** Trả Date thuận tiện cho UserHistoryService */
    public Date getRevisionDate() {
        return new Date(getTimestamp());
    }
}
