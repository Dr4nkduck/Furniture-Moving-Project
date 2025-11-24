package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_quote_settings")
public class AiQuoteSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String currency;

    @Column(name = "price_per_km")
    private BigDecimal pricePerKm;

    @Column(name = "min_fare")
    private BigDecimal minFare;

    @Column(name = "max_days_ahead")
    private Integer maxDaysAhead;

    // getters & setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getPricePerKm() { return pricePerKm; }
    public void setPricePerKm(BigDecimal pricePerKm) { this.pricePerKm = pricePerKm; }

    public BigDecimal getMinFare() { return minFare; }
    public void setMinFare(BigDecimal minFare) { this.minFare = minFare; }

    public Integer getMaxDaysAhead() { return maxDaysAhead; }
    public void setMaxDaysAhead(Integer maxDaysAhead) { this.maxDaysAhead = maxDaysAhead; }
}
