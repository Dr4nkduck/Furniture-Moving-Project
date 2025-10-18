package SWP301.Furniture_Moving_Project.model.converter;

import SWP301.Furniture_Moving_Project.model.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false) // keep explicit usage with @Convert on the field
public class OrderStatusAttributeConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        if (attribute == null) return null;
        return switch (attribute) {
            case PENDING_OFFER -> "pending";
            case ASSIGNED      -> "assigned";
            case IN_PROGRESS   -> "in_progress";
            case COMPLETED     -> "completed";
            case CANCELLED     -> "cancelled";
        };
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        String v = dbValue.trim().toLowerCase().replace('-', '_');
        return switch (v) {
            case "pending", "pending_offer" -> OrderStatus.PENDING_OFFER;
            case "assigned"                 -> OrderStatus.ASSIGNED;
            case "in_progress"              -> OrderStatus.IN_PROGRESS;
            case "completed"                -> OrderStatus.COMPLETED;
            case "cancelled", "canceled"    -> OrderStatus.CANCELLED;
            default                         -> null; // or throw new IllegalArgumentException(v)
        };
    }
}
