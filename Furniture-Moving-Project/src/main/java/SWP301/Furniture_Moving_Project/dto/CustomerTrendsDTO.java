package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CustomerTrendsDTO {

    public static class FunnelPoint {
        private LocalDate date;
        private long searches;
        private long quotes;
        private long bookings;

        public FunnelPoint() {
        }

        public FunnelPoint(LocalDate date, long searches, long quotes, long bookings) {
            this.date = date;
            this.searches = searches;
            this.quotes = quotes;
            this.bookings = bookings;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public long getSearches() {
            return searches;
        }

        public void setSearches(long searches) {
            this.searches = searches;
        }

        public long getQuotes() {
            return quotes;
        }

        public void setQuotes(long quotes) {
            this.quotes = quotes;
        }

        public long getBookings() {
            return bookings;
        }

        public void setBookings(long bookings) {
            this.bookings = bookings;
        }
    }

    public static class CorridorRow {
        private String pickupCity;
        private String deliveryCity;
        private long trips;

        public CorridorRow() {
        }

        public CorridorRow(String pickupCity, String deliveryCity, long trips) {
            this.pickupCity = pickupCity;
            this.deliveryCity = deliveryCity;
            this.trips = trips;
        }

        public String getPickupCity() {
            return pickupCity;
        }

        public void setPickupCity(String pickupCity) {
            this.pickupCity = pickupCity;
        }

        public String getDeliveryCity() {
            return deliveryCity;
        }

        public void setDeliveryCity(String deliveryCity) {
            this.deliveryCity = deliveryCity;
        }

        public long getTrips() {
            return trips;
        }

        public void setTrips(long trips) {
            this.trips = trips;
        }
    }

    public static class CancelReasonRow {
        private String reason;
        private long count;

        public CancelReasonRow() {
        }

        public CancelReasonRow(String reason, long count) {
            this.reason = reason;
            this.count = count;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    private List<FunnelPoint> funnel;
    private List<CorridorRow> topCorridors;
    private BigDecimal aov;
    private List<CancelReasonRow> cancelReasons;

    public CustomerTrendsDTO() {
    }

    public CustomerTrendsDTO(List<FunnelPoint> funnel, List<CorridorRow> topCorridors,
                             BigDecimal aov, List<CancelReasonRow> cancelReasons) {
        this.funnel = funnel;
        this.topCorridors = topCorridors;
        this.aov = aov;
        this.cancelReasons = cancelReasons;
    }

    public List<FunnelPoint> getFunnel() {
        return funnel;
    }

    public void setFunnel(List<FunnelPoint> funnel) {
        this.funnel = funnel;
    }

    public List<CorridorRow> getTopCorridors() {
        return topCorridors;
    }

    public void setTopCorridors(List<CorridorRow> topCorridors) {
        this.topCorridors = topCorridors;
    }

    public BigDecimal getAov() {
        return aov;
    }

    public void setAov(BigDecimal aov) {
        this.aov = aov;
    }

    public List<CancelReasonRow> getCancelReasons() {
        return cancelReasons;
    }

    public void setCancelReasons(List<CancelReasonRow> cancelReasons) {
        this.cancelReasons = cancelReasons;
    }
}
