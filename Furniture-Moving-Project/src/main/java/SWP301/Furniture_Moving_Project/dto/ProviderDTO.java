package SWP301.Furniture_Moving_Project.dto;

import java.math.BigDecimal;

public class ProviderDTO {
    private Long providerId;
    private String companyName;
    private BigDecimal rating;

    public ProviderDTO() {
    }
    public ProviderDTO(Long providerId, String companyName, BigDecimal rating) {
        this.providerId = providerId;
        this.companyName = companyName;
        this.rating = rating;
    }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
}
