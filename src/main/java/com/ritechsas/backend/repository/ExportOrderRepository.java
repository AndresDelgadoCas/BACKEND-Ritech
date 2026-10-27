
package com.ritechsas.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ritechsas.backend.entity.ExportOrder;
import com.ritechsas.backend.entity.ExportOrder.OrderStatus;

public interface ExportOrderRepository
        extends JpaRepository<ExportOrder, Long> {

    Optional<ExportOrder> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    List<ExportOrder> findByClientId(Long clientId);

    List<ExportOrder> findByStatus(OrderStatus status);
}
