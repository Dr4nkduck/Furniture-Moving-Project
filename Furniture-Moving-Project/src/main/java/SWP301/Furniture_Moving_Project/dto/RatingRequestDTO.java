package SWP301.Furniture_Moving_Project.dto;

public class RatingRequestDTO {

    private int rating;     // 1-5
    private String comment; // optional

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
}
