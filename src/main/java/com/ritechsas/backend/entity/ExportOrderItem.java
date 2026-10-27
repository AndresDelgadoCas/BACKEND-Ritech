
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

@Entity
@Table(name = "export_order_items")
public class ExportOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "export_order_id", nullable = false)
    private ExportOrder exportOrder;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "inventory_lot_id", nullable = false)
    private InventoryLot inventoryLot;

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityKg;

    @Column(name = "sale_price_usd_per_kg", nullable = false, precision = 12, scale = 4)
    private BigDecimal salePriceUsdPerKg;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "total_amount_usd", insertable = false, updatable = false,
            precision = 18, scale = 2)
    private BigDecimal totalAmountUsd;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ExportOrderItem() {
    }

    public Long getId() { return id; }
    public ExportOrder getExportOrder() { return exportOrder; }
    public void setExportOrder(ExportOrder exportOrder) { this.exportOrder = exportOrder; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public InventoryLot getInventoryLot() { return inventoryLot; }
    public void setInventoryLot(InventoryLot inventoryLot) { this.inventoryLot = inventoryLot; }
    public BigDecimal getQuantityKg() { return quantityKg; }
    public void setQuantityKg(BigDecimal quantityKg) { this.quantityKg = quantityKg; }
    public BigDecimal getSalePriceUsdPerKg() { return salePriceUsdPerKg; }
    public void setSalePriceUsdPerKg(BigDecimal salePriceUsdPerKg) { this.salePriceUsdPerKg = salePriceUsdPerKg; }
    public BigDecimal getTotalAmountUsd() { return totalAmountUsd; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
