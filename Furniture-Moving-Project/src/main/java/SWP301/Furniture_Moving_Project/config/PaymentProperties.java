package SWP301.Furniture_Moving_Project.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private final Bank bank = new Bank();
    public Bank getBank() { return bank; }

    public static class Bank {
        private String name;
        private String accountNumber;
        private String accountName;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
    }
}
