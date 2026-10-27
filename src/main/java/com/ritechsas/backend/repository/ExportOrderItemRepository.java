
package com.ritechsas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ritechsas.backend.entity.ExportOrderItem;

public interface ExportOrderItemRepository
        extends JpaRepository<ExportOrderItem, Long> {

    List<ExportOrderItem> findByExportOrder_Id(Long orderId);

    List<ExportOrderItem> findByProduct_Id(Long productId);

    List<ExportOrderItem> findByInventoryLot_Id(Long lotId);

    @Query("""
        SELECT COALESCE(SUM(item.quantityKg), 0)
        FROM ExportOrderItem item
        WHERE item.inventoryLot.id = :lotId
          AND item.exportOrder.status NOT IN (
              com.ritechsas.backend.entity.ExportOrder.OrderStatus.DRAFT,
              com.ritechsas.backend.entity.ExportOrder.OrderStatus.CANCELLED
          )
        """)
    java.math.BigDecimal sumCommittedQuantityByLotId(
            @Param("lotId") Long lotId);
}
