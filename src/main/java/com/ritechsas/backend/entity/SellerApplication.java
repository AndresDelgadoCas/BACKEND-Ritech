package com.ritechsas.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "seller_applications")
public class SellerApplication {
    public enum Status { PENDING, APPROVED, REJECTED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable=false) private Long userId;
    @Column(name="business_name", nullable=false, length=180) private String businessName;
    @Column(name="contact_name", nullable=false, length=150) private String contactName;
    @Column(nullable=false, length=150) private String email;
    @Column(length=30) private String phone;
    @Column(nullable=false, length=100) private String country;
    @Column(name="product_type", nullable=false) private String productType;
    @Column(name="business_description", columnDefinition="text") private String businessDescription;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status = Status.PENDING;
    @Column(name="reviewed_by") private Long reviewedBy;
    @Column(name="reviewed_at") private LocalDateTime reviewedAt;
    @Column(name="rejection_reason", length=500) private String rejectionReason;
    @Column(name="producer_id") private Long producerId;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at", insertable=false, updatable=false) private LocalDateTime updatedAt;
    public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getBusinessName(){return businessName;} public void setBusinessName(String v){businessName=v;}
    public String getContactName(){return contactName;} public void setContactName(String v){contactName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getCountry(){return country;} public void setCountry(String v){country=v;}
    public String getProductType(){return productType;} public void setProductType(String v){productType=v;}
    public String getBusinessDescription(){return businessDescription;} public void setBusinessDescription(String v){businessDescription=v;}
    public Status getStatus(){return status;} public void setStatus(Status v){status=v;}
    public Long getReviewedBy(){return reviewedBy;} public void setReviewedBy(Long v){reviewedBy=v;}
    public LocalDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(LocalDateTime v){reviewedAt=v;}
    public String getRejectionReason(){return rejectionReason;} public void setRejectionReason(String v){rejectionReason=v;}
    public Long getProducerId(){return producerId;} public void setProducerId(Long v){producerId=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
