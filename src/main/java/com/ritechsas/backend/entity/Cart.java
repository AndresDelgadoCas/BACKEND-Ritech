package com.ritechsas.backend.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import jakarta.persistence.*;

@Entity
@Table(name = "carts")
public class Cart {
    public enum CartStatus { ACTIVE, CONVERTED, ABANDONED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "buyer_user_id", nullable = false) private Long buyerUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 9) private CartStatus status = CartStatus.ACTIVE;
    @Generated(event = EventType.INSERT) @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Generated(event = {EventType.INSERT, EventType.UPDATE}) @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
    public Cart() {}
    public Long getId() { return id; }
    public Long getBuyerUserId() { return buyerUserId; }
    public void setBuyerUserId(Long value) { buyerUserId = value; }
    public CartStatus getStatus() { return status; }
    public void setStatus(CartStatus value) { status = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
