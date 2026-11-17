package com.ritechsas.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(name = "uq_cart_listing", columnNames = {"cart_id", "listing_id"}))
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "cart_id", nullable = false) private Cart cart;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "listing_id", nullable = false) private ProductListing listing;
    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 3) private BigDecimal quantityKg;
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime addedAt;
    public CartItem() {}
    public Long getId() { return id; }
    public Cart getCart() { return cart; }
    public void setCart(Cart value) { cart = value; }
    public ProductListing getListing() { return listing; }
    public void setListing(ProductListing value) { listing = value; }
    public BigDecimal getQuantityKg() { return quantityKg; }
    public void setQuantityKg(BigDecimal value) { quantityKg = value; }
    public LocalDateTime getAddedAt() { return addedAt; }
}
