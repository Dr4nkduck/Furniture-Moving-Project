package SWP301.Furniture_Moving_Project.model;

public final class RequestStatuses {
    private RequestStatuses(){}

    public static final String PENDING        = "pending";
    public static final String PENDING_OFFER  = "pending_offer";
    public static final String ASSIGNED       = "assigned";
    public static final String IN_PROGRESS    = "in_progress";
    public static final String COMPLETED      = "completed";
    public static final String CANCELLED      = "cancelled";

    public static boolean isValid(String s) {
        if (s == null) return false;
        return switch (s) {
            case PENDING, PENDING_OFFER, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED -> true;
            default -> false;
        };
    }
}
