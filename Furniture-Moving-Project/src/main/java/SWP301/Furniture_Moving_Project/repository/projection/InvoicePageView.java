package SWP301.Furniture_Moving_Project.repository.projection;

public interface InvoicePageView {
    Integer getInvoiceId();
    String  getInvoiceDate();        // dd-MM-yyyy
    String  getStatus();             // pending/assigned/in_progress/completed/cancelled
    String  getTotalCost();
    String  getProviderCompany();    // có thể null
    String  getPickupAddress();
    String  getDeliveryAddress();
    String  getFurnitureItemsJson(); // JSON array string
}
