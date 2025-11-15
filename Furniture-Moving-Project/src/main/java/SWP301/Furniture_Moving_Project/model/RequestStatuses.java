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

    /** Có phải trạng thái kết thúc (không cho đổi nữa) không? */
    public static boolean isTerminal(String s) {
        if (s == null) return false;
        return COMPLETED.equals(s) || CANCELLED.equals(s);
    }

    /**
     * Luật chuyển trạng thái:
     * - PENDING        -> PENDING_OFFER, CANCELLED
     * - PENDING_OFFER  -> ASSIGNED, CANCELLED
     * - ASSIGNED       -> IN_PROGRESS, CANCELLED
     * - IN_PROGRESS    -> COMPLETED, CANCELLED
     * - COMPLETED      -> (không được chuyển)
     * - CANCELLED      -> (không được chuyển)
     */
    public static boolean canTransition(String from, String to) {
        if (!isValid(from) || !isValid(to)) return false;
        if (from.equals(to)) return true; // không đổi gì thì cho qua

        return switch (from) {
            case PENDING ->
                    to.equals(PENDING_OFFER) || to.equals(CANCELLED);
            case PENDING_OFFER ->
                    to.equals(ASSIGNED) || to.equals(CANCELLED);
            case ASSIGNED ->
                    to.equals(IN_PROGRESS) || to.equals(CANCELLED);
            case IN_PROGRESS ->
                    to.equals(COMPLETED) || to.equals(CANCELLED);
            case COMPLETED, CANCELLED ->
                    false; // ❌ đã kết thúc, không cho đổi nữa
            default -> false;
        };
    }
}
