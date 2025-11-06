package SWP301.Furniture_Moving_Project.repository.projection;

public interface ProviderOrderItemRow {
    Integer getItemId();
    String  getItemType();
    String  getSize();
    Integer getQuantity();
    Boolean getIsFragile();
}
