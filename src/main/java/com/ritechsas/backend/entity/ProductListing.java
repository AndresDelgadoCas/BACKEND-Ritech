package com.ritechsas.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import jakarta.persistence.*;

@Entity
@Table(name = "product_listings")
public class ProductListing {
    public enum ListingStatus { DRAFT, PUBLISHED, PAUSED, SOLD_OUT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producer_id", nullable = false)
    private Producer producer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "inventory_lot_id", nullable = false)
    private InventoryLot inventoryLot;

    @Column(nullable = false, length = 180) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "price_usd_per_kg", nullable = false, precision = 12, scale = 2) private BigDecimal priceUsdPerKg;
    @Column(name = "minimum_order_kg", nullable = false, precision = 12, scale = 2) private BigDecimal minimumOrderKg = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 9)
    private ListingStatus status = ListingStatus.DRAFT;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public ProductListing() {}
    public Long getId() { return id; }
    public Producer getProducer() { return producer; }
    public void setProducer(Producer producer) { this.producer = producer; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public InventoryLot getInventoryLot() { return inventoryLot; }
    public void setInventoryLot(InventoryLot inventoryLot) { this.inventoryLot = inventoryLot; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPriceUsdPerKg() { return priceUsdPerKg; }
    public void setPriceUsdPerKg(BigDecimal value) { this.priceUsdPerKg = value; }
    public BigDecimal getMinimumOrderKg() { return minimumOrderKg; }
    public void setMinimumOrderKg(BigDecimal value) { this.minimumOrderKg = value; }
    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
