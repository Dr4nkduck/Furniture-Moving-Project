package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.CreateFullRequestDTO;
import SWP301.Furniture_Moving_Project.dto.RequestMetaDTO;
import SWP301.Furniture_Moving_Project.model.Address;
import SWP301.Furniture_Moving_Project.model.Customer;
import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import SWP301.Furniture_Moving_Project.model.RequestAddress;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.AddressRepository;
import SWP301.Furniture_Moving_Project.repository.CustomerRepository;
import SWP301.Furniture_Moving_Project.repository.FurnitureItemRepository;
import SWP301.Furniture_Moving_Project.repository.RequestAddressRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class FullRequestService {

    private final ServiceRequestRepository serviceRequestRepo;
    private final AddressRepository addressRepo;
    private final RequestAddressRepository requestAddressRepo;
    private final FurnitureItemRepository furnitureItemRepo;
    private final CustomerRepository customerRepo;

    public FullRequestService(ServiceRequestRepository serviceRequestRepo,
                              AddressRepository addressRepo,
                              RequestAddressRepository requestAddressRepo,
                              FurnitureItemRepository furnitureItemRepo,
                              CustomerRepository customerRepo) {
        this.serviceRequestRepo = serviceRequestRepo;
        this.addressRepo = addressRepo;
        this.requestAddressRepo = requestAddressRepo;
        this.furnitureItemRepo = furnitureItemRepo;
        this.customerRepo = customerRepo;
    }

    /**
     * Flow B:
     * - Không yêu cầu addressId từ FE.
     * - Tự tạo 2 Address (pickup/delivery) gắn vào ServiceRequest (FK tới addresses).
     * - Snapshot metadata địa chỉ vào request_addresses.
     * - Lưu danh sách furniture_items.
     */
    @Transactional
    public Integer createAll(CreateFullRequestDTO dto) {
        // 1) Lấy meta chính
        RequestMetaDTO meta = dto.getRequest();
        Integer customerId    = meta.getCustomerId();
        Integer providerId    = meta.getProviderId();
        var     preferredDate = meta.getPreferredDate();

        // 2) Resolve userId từ customerId để tạo Address
        Customer cus = customerRepo.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid customerId: " + customerId));
        Integer userId = cus.getUserId();

        // 3) Tạo 2 bản ghi Address (tối thiểu)
        Address pickupAddr = toAddress(
                userId, "home",
                dto.getPickupAddress().getAddressLine1(),
                dto.getPickupAddress().getCity(),
                dto.getPickupAddress().getDistrict(),
                "00000"
        );
        addressRepo.save(pickupAddr);

        Address deliveryAddr = toAddress(
                userId, "delivery",
                dto.getDeliveryAddress().getAddressLine1(),
                dto.getDeliveryAddress().getCity(),
                dto.getDeliveryAddress().getDistrict(),
                "00000"
        );
        addressRepo.save(deliveryAddr);

        // 4) Tạo ServiceRequest và gắn 2 Address (ManyToOne)
        ServiceRequest sr = new ServiceRequest();
        sr.setCustomerId(customerId);
        sr.setProviderId(providerId);                 // có thể null
        sr.setPreferredDate(preferredDate);
        sr.setStatus("pending");
        sr.setTotalCost(meta.getEstimatedCost());     // nếu FE gửi estimate
        sr.setPickupAddress(pickupAddr);
        sr.setDeliveryAddress(deliveryAddr);

        // Dùng biến final để tránh lỗi "effectively final" ở lambda
        final ServiceRequest saved = serviceRequestRepo.save(sr);

        // 5) Snapshot địa chỉ vào request_addresses
        saveRequestAddress(saved, "PICKUP", dto.getPickupAddress());
        saveRequestAddress(saved, "DELIVERY", dto.getDeliveryAddress());

        // 6) Lưu furniture_items
        if (dto.getFurnitureItems() != null) {
            dto.getFurnitureItems().forEach(it -> {
                FurnitureItem fi = new FurnitureItem();
                fi.setServiceRequest(saved); // FK -> request_id
                fi.setItemType(Objects.toString(it.getName(), "Item"));
                fi.setSize(composeSize(it.getLengthCm(), it.getWidthCm(), it.getHeightCm(), it.getWeightKg()));
                fi.setQuantity(it.getQuantity() != null ? it.getQuantity() : 1);
                fi.setIsFragile(Boolean.TRUE.equals(it.getFragile()));
                // special_handling: có thể map từ ghi chú nếu bạn muốn
                furnitureItemRepo.save(fi);
            });
        }

        return saved.getRequestId();
    }

    // ---------- Helpers ----------

    private Address toAddress(Integer userId, String type, String street, String city, String state, String zip) {
        Address a = new Address();
        a.setUserId(userId);
        a.setAddressType(type != null ? type : "home");
        a.setStreetAddress(street);
        a.setCity(city != null ? city : "");
        a.setState(state != null ? state : "");
        a.setZipCode(zip != null ? zip : "00000");
        return a;
    }

    private void saveRequestAddress(ServiceRequest req, String type, CreateFullRequestDTO.AddressPayload p) {
        RequestAddress ra = new RequestAddress();
        ra.setServiceRequest(req);
        ra.setAddressType(type);
        ra.setStreetAddress(p.getAddressLine1());
        ra.setCity(p.getCity());
        ra.setState(p.getDistrict()); // FE "district" -> map sang state theo schema hiện tại
        ra.setZipCode(null);
        ra.setContactName(p.getContactName());
        ra.setContactPhone(p.getContactPhone());
        // Nếu entity của bạn đã có trường specialInstructions:
        ra.setSpecialInstructions(p.getSpecialInstructions());
        // Nếu entity của bạn có floor/hasElevator thì thêm:
        // ra.setFloor(p.getFloor());
        // ra.setHasElevator(Boolean.TRUE.equals(p.getHasElevator()));

        requestAddressRepo.save(ra);
    }

    private String composeSize(Integer len, Integer wid, Integer hgt, BigDecimal weightKg) {
        String l = len != null ? String.valueOf(len) : "0";
        String w = wid != null ? String.valueOf(wid) : "0";
        String h = hgt != null ? String.valueOf(hgt) : "0";
        String s = l + "x" + w + "x" + h + " cm";
        if (weightKg != null) s += "; " + weightKg + "kg";
        return s;
    }
}
