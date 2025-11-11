package SWP301.Furniture_Moving_Project.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private String addInfoPrefix = "REQ";
    private int expireMinutes = 15;

    public String getAddInfoPrefix() {
        return addInfoPrefix;
    }

    public void setAddInfoPrefix(String addInfoPrefix) {
        this.addInfoPrefix = addInfoPrefix;
    }

    public int getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(int expireMinutes) {
        this.expireMinutes = expireMinutes;
    }
}
