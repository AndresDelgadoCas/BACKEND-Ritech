
package com.ritechsas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import com.ritechsas.backend.entity.InventoryLot;

public interface InventoryLotRepository
        extends JpaRepository<InventoryLot, Long> {

    List<InventoryLot> findByStatus(InventoryLot.LotStatus status);

    boolean existsByLotCode(String lotCode);

    List<InventoryLot> findByProducer_UserId(Long userId);

    List<InventoryLot> findByProducer_Id(Long producerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lot FROM InventoryLot lot WHERE lot.id = :id")
    Optional<InventoryLot> findByIdForUpdate(@Param("id") Long id);

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
