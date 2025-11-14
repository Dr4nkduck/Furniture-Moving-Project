// src/main/java/SWP301/Furniture_Moving_Project/model/converter/AccountStatusConverter.java
package SWP301.Furniture_Moving_Project.model.converter;

import SWP301.Furniture_Moving_Project.model.AccountStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountStatusConverter implements AttributeConverter<AccountStatus, String> {

    @Override
    public String convertToDatabaseColumn(AccountStatus attr) {
        return attr == null ? "active" : attr.name().toLowerCase();
    }

    @Override
    public AccountStatus convertToEntityAttribute(String db) {
        if (db == null) return AccountStatus.ACTIVE;
        return switch (db.toLowerCase()) {
            case "suspended" -> AccountStatus.SUSPENDED; // ✅ đổi từ LOCKED → SUSPENDED
            case "deleted"   -> AccountStatus.DELETED;
            default          -> AccountStatus.ACTIVE;
        };
    }
}
