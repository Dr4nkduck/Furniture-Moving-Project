package SWP301.Furniture_Moving_Project.service;

public interface ActivityLogService {

    /**
     * Generic audit logger.
     * @param actorId  the acting user/provider id (nullable)
     * @param type     short action key, e.g. "ORDER_PII_READ", "ORDER_STATUS_UPDATE"
     * @param details  free-form details (e.g. "providerId=12, orderId=99, to=IN_PROGRESS")
     */
    void log(Long actorId, String type, String details);

    /* -------- Optional helpers (syntactic sugar) -------- */

    default void logProvider(Long providerId, String type, String details) {
        log(providerId, type, details);
    }

    default void logOrderPiiRead(Long providerId, Long orderId) {
        log(providerId, "ORDER_PII_READ", "orderId=" + orderId);
    }

    default void logOrderStatusUpdate(Long providerId, Long orderId, String to) {
        log(providerId, "ORDER_STATUS_UPDATE", "orderId=" + orderId + ", to=" + to);
    }
}
