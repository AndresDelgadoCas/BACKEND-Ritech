
package com.ritechsas.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ritechsas.backend.entity.Shipment;
import com.ritechsas.backend.entity.Shipment.ShipmentStatus;

public interface ShipmentRepository
        extends JpaRepository<Shipment, Long> {

    List<Shipment> findByExportOrder_Id(Long orderId);

    List<Shipment> findByStatus(ShipmentStatus status);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);
}
