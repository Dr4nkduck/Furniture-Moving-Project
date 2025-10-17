package SWP301.Furniture_Moving_Project.model;

public enum OrderStatus {
    RFP,          // request for proposal (pending offer)
    OFFERED,      // customer received an offer
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}