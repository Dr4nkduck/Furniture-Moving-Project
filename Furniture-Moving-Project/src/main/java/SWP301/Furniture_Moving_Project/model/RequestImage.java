package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "request_images")
public class RequestImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest serviceRequest;

    // map đúng tên cột trong DB
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // ❌ KHÔNG map cột 'data' vì DB không có
    // Nếu vẫn muốn giữ trường này ở FE/BE để xử lý tạm thời, đánh dấu @Transient
    @Transient
    private byte[] data;

    // Getters/Setters
    public Long getId() { return id; }
    public ServiceRequest getServiceRequest() { return serviceRequest; }
    public void setServiceRequest(ServiceRequest serviceRequest) { this.serviceRequest = serviceRequest; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
