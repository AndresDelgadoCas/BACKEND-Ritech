
package com.ritechsas.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producer_id", nullable = false)
    private Producer producer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inventory_lot_id")
    private InventoryLot inventoryLot;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityKg;

    @Column(name = "purchase_price_usd_per_kg",
            nullable = false, precision = 12, scale = 4)
    private BigDecimal purchasePriceUsdPerKg;

    @Column(name = "total_amount_usd",
            insertable = false, updatable = false,
            precision = 18, scale = 2)
    private BigDecimal totalAmountUsd;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public Purchase() {
    }

    public Long getId() {
        return id;
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public InventoryLot getInventoryLot() {
        return inventoryLot;
    }

    public void setInventoryLot(InventoryLot inventoryLot) {
        this.inventoryLot = inventoryLot;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getQuantityKg() {
        return quantityKg;
    }

    public void setQuantityKg(BigDecimal quantityKg) {
        this.quantityKg = quantityKg;
    }

    public BigDecimal getPurchasePriceUsdPerKg() {
        return purchasePriceUsdPerKg;
    }

    public void setPurchasePriceUsdPerKg(BigDecimal purchasePriceUsdPerKg) {
        this.purchasePriceUsdPerKg = purchasePriceUsdPerKg;
    }

    public BigDecimal getTotalAmountUsd() {
        return totalAmountUsd;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
