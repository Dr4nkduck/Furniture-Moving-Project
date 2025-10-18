package SWP301.Furniture_Moving_Project.model;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Order lifecycle for Provider side.
 *
 * PENDING_OFFER -> ASSIGNED -> IN_PROGRESS -> COMPLETED
 *                                  \-> CANCELLED
 */
public enum OrderStatus {
    PENDING_OFFER,   // awaiting provider offer/accept
    ASSIGNED,        // assigned to a provider, not started
    IN_PROGRESS,     // provider is executing the job
    COMPLETED,       // finished successfully
    CANCELLED;       // cancelled by provider/customer/system

    /** Allowed forward transitions for each status */
    public Set<OrderStatus> nextAllowed() {
        return switch (this) {
            case PENDING_OFFER -> EnumSet.of(ASSIGNED);                 // accept offer
            case ASSIGNED      -> EnumSet.of(IN_PROGRESS, CANCELLED);   // start or cancel
            case IN_PROGRESS   -> EnumSet.of(COMPLETED, CANCELLED);     // finish or cancel
            case COMPLETED     -> EnumSet.noneOf(OrderStatus.class);    // terminal
            case CANCELLED     -> EnumSet.noneOf(OrderStatus.class);    // terminal
        };
    }

    /** Validate if moving from 'from' to 'to' is legal */
    public static boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) return false;
        return from.nextAllowed().contains(to);
    }

    /** Case-insensitive, underscores or hyphens both accepted */
    public static OrderStatus safeParse(String value) {
        if (value == null) return null;
        String norm = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return OrderStatus.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Human-friendly label (optional, for UI) */
    public String label() {
        return switch (this) {
            case PENDING_OFFER -> "Pending Offer";
            case ASSIGNED      -> "Assigned";
            case IN_PROGRESS   -> "In Progress";
            case COMPLETED     -> "Completed";
            case CANCELLED     -> "Cancelled";
        };
    }
}
