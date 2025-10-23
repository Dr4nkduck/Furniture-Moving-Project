package SWP301.Furniture_Moving_Project.model.converter;

import SWP301.Furniture_Moving_Project.model.AccountStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountStatusConverter implements AttributeConverter<AccountStatus, String> {
    @Override
    public String convertToDatabaseColumn(AccountStatus attribute) {
        if (attribute == null) return null;
        return switch (attribute) {
            case ACTIVE -> "active";
            case SUSPENDED -> "suspended";
            case DELETED -> "deleted";
        };
    }

    @Override
    public AccountStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData.toLowerCase()) {
            case "active" -> AccountStatus.ACTIVE;
            case "suspended" -> AccountStatus.SUSPENDED;
            case "deleted" -> AccountStatus.DELETED;
            default -> throw new IllegalArgumentException("Unknown status: " + dbData);
        };
    }
}
