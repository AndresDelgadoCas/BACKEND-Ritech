package com.ritechsas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ritechsas.backend.entity.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByProducerId(Long producerId);

    List<Purchase> findByProductId(Long productId);

    List<Purchase> findByInventoryLotId(Long inventoryLotId);
}