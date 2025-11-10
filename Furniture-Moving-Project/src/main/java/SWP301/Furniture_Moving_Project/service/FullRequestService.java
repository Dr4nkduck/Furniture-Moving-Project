package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.CreateFullRequestDTO;
import SWP301.Furniture_Moving_Project.dto.RequestMetaDTO;
import SWP301.Furniture_Moving_Project.model.Address;
import SWP301.Furniture_Moving_Project.model.Customer;
import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import SWP301.Furniture_Moving_Project.model.RequestAddress;
import SWP301.Furniture_Moving_Project.model.RequestImage;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.AddressRepository;
import SWP301.Furniture_Moving_Project.repository.CustomerRepository;
import SWP301.Furniture_Moving_Project.repository.FurnitureItemRepository;
import SWP301.Furniture_Moving_Project.repository.RequestAddressRepository;
import SWP301.Furniture_Moving_Project.repository.RequestImageRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class FullRequestService {

    private final ServiceRequestRepository serviceRequestRepo;
    private final AddressRepository addressRepo;
    private final RequestAddressRepository requestAddressRepo;
    private final FurnitureItemRepository furnitureItemRepo;
    private final CustomerRepository customerRepo;
    private final RequestImageRepository requestImageRepo;

    /** Thư mục gốc để lưu file vật lý (trên máy chủ). Ví dụ: uploads */
    @Value("${app.upload.dir:uploads}")
    private String uploadRoot;

    /** Base URL public để client truy cập ảnh. Ví dụ: /uploads hoặc https://cdn.yourdomain.com/uploads */
    @Value("${app.upload.public-base:/uploads}")
    private String publicBaseUrl;

    public FullRequestService(ServiceRequestRepository serviceRequestRepo,
                              AddressRepository addressRepo,
                              RequestAddressRepository requestAddressRepo,
                              FurnitureItemRepository furnitureItemRepo,
                              CustomerRepository customerRepo,
                              RequestImageRepository requestImageRepo) {
        this.serviceRequestRepo = serviceRequestRepo;
        this.addressRepo = addressRepo;
        this.requestAddressRepo = requestAddressRepo;
        this.furnitureItemRepo = furnitureItemRepo;
        this.customerRepo = customerRepo;
        this.requestImageRepo = requestImageRepo;
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
        Integer userId = cus.getUser().getUserId();

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
                furnitureItemRepo.save(fi);
            });
        }

        return saved.getRequestId();
    }

    /**
     * Lưu nhiều ảnh cho 1 request:
     * - Lưu file ra đĩa theo thư mục ngày (yyyy/MM/dd).
     * - Lưu metadata vào bảng request_images (original_name, stored_name, url, content_type, size).
     * - KHÔNG ghi vào cột 'data' vì schema không có.
     */
    @Transactional
    public int saveImagesToRequest(Integer requestId, List<MultipartFile> images) {
        ServiceRequest req = serviceRequestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        if (images == null || images.isEmpty()) return 0;

        // Thư mục theo ngày
        String dated = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .withZone(ZoneId.systemDefault())
                .format(java.time.Instant.now());

        Path datedDir = Paths.get(uploadRoot, dated);
        try {
            Files.createDirectories(datedDir);
        } catch (Exception e) {
            throw new RuntimeException("Không tạo được thư mục upload: " + datedDir, e);
        }

        int saved = 0;
        for (MultipartFile mf : images) {
            if (mf == null || mf.isEmpty()) continue;

            String original = Optional.ofNullable(mf.getOriginalFilename()).orElse("file");
            String cleanOriginal = original.replaceAll("[\\s]+", "_");
            // Tên file lưu trữ duy nhất
            String storedName = UUID.randomUUID() + "-" + cleanOriginal;

            Path dest = datedDir.resolve(storedName);
            try {
                Files.copy(mf.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new RuntimeException("Ghi file thất bại: " + dest, e);
            }

            // URL public cho FE (ví dụ: /uploads/2025/10/27/<storedName>)
            String url = buildPublicUrl(dated, storedName);

            RequestImage img = new RequestImage();
            img.setServiceRequest(req);
            img.setOriginalName(original);
            img.setStoredName(storedName);
            img.setUrl(url);
            img.setContentType(mf.getContentType());
            img.setSize(mf.getSize());
            requestImageRepo.save(img);

            saved++;
        }
        return saved;
    }

    private String buildPublicUrl(String datedFolder, String storedName) {
        String base = Optional.ofNullable(publicBaseUrl).orElse("").trim();
        // Chuẩn hoá: bỏ dấu '/' dư ở hai đầu rồi ghép
        String prefix = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String dated = datedFolder.startsWith("/") ? datedFolder.substring(1) : datedFolder;
        return prefix + "/" + dated + "/" + storedName;
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
        ra.setSpecialInstructions(p.getSpecialInstructions());
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
