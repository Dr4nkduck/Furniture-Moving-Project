package SWP301.Furniture_Moving_Project.dto;

import SWP301.Furniture_Moving_Project.model.User;

import java.util.Date;

public class UserHistoryDto {
    private Long revisionId;
    private String modifiedBy;
    private Date changedAt;
    private String action; // INSERT / UPDATE / DELETE
    private User userData;

    public UserHistoryDto(Long revisionId, String modifiedBy, Date changedAt, String action, User userData) {
        this.revisionId = revisionId;
        this.modifiedBy = modifiedBy;
        this.changedAt = changedAt;
        this.action = action;
        this.userData = userData;
    }

    public User getUserData() {
        return userData;
    }

    public void setUserData(User userData) {
        this.userData = userData;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Date getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Date changedAt) {
        this.changedAt = changedAt;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Long getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(Long revisionId) {
        this.revisionId = revisionId;
    }
}
