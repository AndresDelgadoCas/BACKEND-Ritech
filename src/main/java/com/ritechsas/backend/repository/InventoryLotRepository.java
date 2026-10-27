
package com.ritechsas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ritechsas.backend.entity.InventoryLot;

public interface InventoryLotRepository
        extends JpaRepository<InventoryLot, Long> {

    List<InventoryLot> findByStatus(InventoryLot.LotStatus status);

    boolean existsByLotCode(String lotCode);

    @Query("""
        SELECT p.productType, SUM(l.quantity)
        FROM InventoryLot l
        JOIN l.product p
        WHERE l.status = :status
        GROUP BY p.productType
        """)
    List<Object[]> sumStockByProductTypeAndStatus(
            @Param("status") InventoryLot.LotStatus status);
}
