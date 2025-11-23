package SWP301.Furniture_Moving_Project.service;

import SWP301.Furniture_Moving_Project.dto.CreateServiceRequestDTO;
import SWP301.Furniture_Moving_Project.dto.FurnitureItemDTO;
import SWP301.Furniture_Moving_Project.model.Address;
import SWP301.Furniture_Moving_Project.model.Customer;
import SWP301.Furniture_Moving_Project.model.FurnitureItem;
import SWP301.Furniture_Moving_Project.model.ServiceRequest;
import SWP301.Furniture_Moving_Project.repository.AddressRepository;
import SWP301.Furniture_Moving_Project.repository.CustomerRepository;
import SWP301.Furniture_Moving_Project.repository.ProviderRepository;
import SWP301.Furniture_Moving_Project.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final AddressRepository addressRepository;
    private final ProviderRepository providerRepository;
    private final CustomerRepository customerRepository;

    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository,
                                 AddressRepository addressRepository,
                                 ProviderRepository providerRepository,
                                 CustomerRepository customerRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.addressRepository = addressRepository;
        this.providerRepository = providerRepository;
        this.customerRepository = customerRepository;
    }

    // ========================
    // Tạo ServiceRequest (giữ nguyên logic của bạn)
    // ========================
    @Transactional
    public Integer create(CreateServiceRequestDTO dto) {

        // 1) Validate provider (nếu có)
        if (dto.getProviderId() != null && !providerRepository.existsById(dto.getProviderId())) {
            throw new IllegalArgumentException("Provider not found: id=" + dto.getProviderId());
        }

        // 2) Lấy customer -> userId để kiểm tra sở hữu địa chỉ
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: id=" + dto.getCustomerId()));
        Integer expectedUserId = customer.getUser().getUserId();

        // 3) Tìm địa chỉ
        Address pickup = addressRepository.findById(dto.getPickupAddressId())
                .orElseThrow(() -> new IllegalArgumentException("Pickup address not found: id=" + dto.getPickupAddressId()));
        Address delivery = addressRepository.findById(dto.getDeliveryAddressId())
                .orElseThrow(() -> new IllegalArgumentException("Delivery address not found: id=" + dto.getDeliveryAddressId()));

        // 4) Kiểm tra ownership: addresses.user_id == customers.user_id
        if (!Objects.equals(pickup.getUserId(), expectedUserId) || !Objects.equals(delivery.getUserId(), expectedUserId)) {
            throw new IllegalArgumentException("Addresses do not belong to this user's account");
        }

        // 5) Tạo ServiceRequest
        ServiceRequest sr = new ServiceRequest();
        sr.setCustomerId(dto.getCustomerId());
        sr.setProviderId(dto.getProviderId());
        sr.setPreferredDate(dto.getPreferredDate());
        // sr.setStatus(dto.getStatus() == null || dto.getStatus().isBlank() ? "pending" : dto.getStatus());
        sr.setTotalCost(dto.getTotalCost());

        // set ManyToOne addresses (entity đã managed)
        sr.setPickupAddress(pickup);
        sr.setDeliveryAddress(delivery);

        // 6) Map items (nếu có)
        List<FurnitureItem> items = new ArrayList<>();
        if (dto.getFurnitureItems() != null) {
            for (FurnitureItemDTO f : dto.getFurnitureItems()) {
                FurnitureItem it = new FurnitureItem();
                it.setServiceRequest(sr);
                it.setItemType(f.getItemType());
                it.setSize(f.getSize());
                it.setQuantity(f.getQuantity());
                it.setIsFragile(f.getIsFragile());
                it.setSpecialHandling(f.getSpecialHandling());
                items.add(it);
            }
        }
        sr.setFurnitureItems(items);

        // 7) Lưu & trả id
        final ServiceRequest saved = serviceRequestRepository.save(sr);
        return saved.getRequestId();
    }

    // ========================
    // Tiện ích chung
    // ========================
    public Optional<ServiceRequest> findById(Integer requestId) {
        return serviceRequestRepository.findById(requestId);
    }

    public ServiceRequest save(ServiceRequest sr) {
        return serviceRequestRepository.save(sr);
    }

    // ========================
    // Thanh toán: đánh dấu đã trả (PAID)
    // ========================
    /**
     * Đánh dấu ServiceRequest đã thanh toán thành công.
     * - Cập nhật: payment_status=PAID, paid_at=now, payment_type, deposit_amount (nếu DEPOSIT_20)
     * - Đồng bộ trạng thái workflow tổng thể: status=PAID (nếu trước đó chưa là PAID)
     */
    @Transactional
    public ServiceRequest markAsPaid(Integer requestId, BigDecimal amount, String paymentType) {
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ServiceRequest #" + requestId));

        // Ghi nhận thông tin thanh toán
        sr.setPaymentStatus("PAID");
        sr.setPaidAt(LocalDateTime.now());
        sr.setPaymentType(paymentType);

        // Nếu chọn đặt cọc 20% thì lưu số tiền đặt cọc, ngược lại để null
        if ("DEPOSIT_20".equalsIgnoreCase(paymentType)) {
            sr.setDepositAmount(amount);
        } else {
            sr.setDepositAmount(null);
        }

        // Đồng bộ trạng thái workflow tổng thể
        if (sr.getStatus() == null || !"PAID".equalsIgnoreCase(sr.getStatus())) {
            sr.setStatus("PAID");
        }

        return serviceRequestRepository.save(sr);
    }

    // (Tuỳ chọn) Nếu cần reset/đổi trạng thái thanh toán (FAILED/EXPIRED) trong tương lai:
    @Transactional
    public ServiceRequest markPaymentStatus(Integer requestId, String paymentStatus) {
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ServiceRequest #" + requestId));
        sr.setPaymentStatus(paymentStatus);
        return serviceRequestRepository.save(sr);
    }
}
