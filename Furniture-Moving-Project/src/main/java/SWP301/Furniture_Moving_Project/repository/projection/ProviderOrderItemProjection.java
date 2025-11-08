package SWP301.Furniture_Moving_Project.repository.projection;

public interface ProviderOrderItemProjection {
    Integer getItemId();
    String getItemType();
    String getSize();
    Integer getQuantity();
    Boolean getIsFragile();
}
