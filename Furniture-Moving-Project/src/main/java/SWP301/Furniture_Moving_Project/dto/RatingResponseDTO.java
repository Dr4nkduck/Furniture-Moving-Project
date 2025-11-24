package SWP301.Furniture_Moving_Project.dto;

public class RatingResponseDTO {

    private int rating;
    private String comment;
    private String createdAt; // ví dụ: "14/11/2025 21:30"

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
