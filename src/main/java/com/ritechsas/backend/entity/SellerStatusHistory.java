package com.ritechsas.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name="seller_status_history")
public class SellerStatusHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="producer_id", nullable=false) private Long producerId;
    @Column(name="changed_by_user_id") private Long changedByUserId;
    @Column(name="previous_status", length=9) private String previousStatus;
    @Column(name="new_status", nullable=false, length=9) private String newStatus;
    @Column(nullable=false, length=500) private String reason;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    public Long getId(){return id;} public Long getProducerId(){return producerId;} public void setProducerId(Long v){producerId=v;}
    public Long getChangedByUserId(){return changedByUserId;} public void setChangedByUserId(Long v){changedByUserId=v;}
    public String getPreviousStatus(){return previousStatus;} public void setPreviousStatus(String v){previousStatus=v;}
    public String getNewStatus(){return newStatus;} public void setNewStatus(String v){newStatus=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
