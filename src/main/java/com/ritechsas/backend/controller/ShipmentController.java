
package com.ritechsas.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ritechsas.backend.dto.ShipmentRequest;
import com.ritechsas.backend.entity.ExportOrder;
import com.ritechsas.backend.entity.ExportOrder.OrderStatus;
import com.ritechsas.backend.entity.Shipment;
import com.ritechsas.backend.entity.Shipment.ShipmentStatus;
import com.ritechsas.backend.repository.ExportOrderRepository;
import com.ritechsas.backend.repository.ShipmentRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentRepository shipmentRepository;
    private final ExportOrderRepository orderRepository;

    public ShipmentController(
            ShipmentRepository shipmentRepository,
            ExportOrderRepository orderRepository) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public List<Shipment> getAll(
            @RequestParam(required = false) Long exportOrderId,
            @RequestParam(required = false) ShipmentStatus status) {

        if (exportOrderId != null) {
            if (!orderRepository.existsById(exportOrderId)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found");
            }

            List<Shipment> shipments =
                    shipmentRepository.findByExportOrder_Id(exportOrderId);

            if (status != null) {
                return shipments.stream()
                        .filter(shipment -> shipment.getStatus() == status)
                        .toList();
            }

            return shipments;
        }

        if (status != null) {
            return shipmentRepository.findByStatus(status);
        }

        return shipmentRepository.findAll();
    }

    @GetMapping("/{id}")
    public Shipment getById(@PathVariable Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shipment not found"));
    }

    @GetMapping("/tracking/{trackingNumber}")
    public Shipment getByTrackingNumber(
            @PathVariable String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shipment not found"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Shipment> create(
            @Valid @RequestBody ShipmentRequest request) {

        validateRequest(request);

        ExportOrder order = orderRepository.findById(request.exportOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found"));

        validateTrackingNumber(request.trackingNumber(), null);

        Shipment shipment = new Shipment();
        shipment.setExportOrder(order);
        copyRequest(shipment, request);

        Shipment saved = shipmentRepository.saveAndFlush(shipment);
        synchronizeOrderStatus(order);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public Shipment update(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentRequest request) {

        validateRequest(request);

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shipment not found"));

        ExportOrder previousOrder = shipment.getExportOrder();

        ExportOrder newOrder = orderRepository.findById(request.exportOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found"));

        validateTrackingNumber(request.trackingNumber(), id);

        shipment.setExportOrder(newOrder);
        copyRequest(shipment, request);

        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        synchronizeOrderStatus(previousOrder);
        if (!previousOrder.getId().equals(newOrder.getId())) {
            synchronizeOrderStatus(newOrder);
        }

        return saved;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shipment not found"));

        ExportOrder order = shipment.getExportOrder();

        shipmentRepository.delete(shipment);
        shipmentRepository.flush();

        synchronizeOrderStatus(order);

        return ResponseEntity.noContent().build();
    }

    private void copyRequest(Shipment shipment, ShipmentRequest request) {
        shipment.setTrackingNumber(clean(request.trackingNumber()));
        shipment.setCarrier(clean(request.carrier()));
        shipment.setShipmentDate(request.shipmentDate());
        shipment.setEstimatedDeliveryDate(request.estimatedDeliveryDate());
        shipment.setActualDeliveryDate(request.actualDeliveryDate());
        shipment.setNotes(clean(request.notes()));

        ShipmentStatus status = request.status() == null
                ? ShipmentStatus.PENDING
                : request.status();

        shipment.setStatus(status);
    }

    private void validateRequest(ShipmentRequest request) {
        validateDates(request);

        ShipmentStatus status = request.status() == null
                ? ShipmentStatus.PENDING
                : request.status();

        if (status == ShipmentStatus.DELIVERED
                && request.actualDeliveryDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Actual delivery date is required for DELIVERED shipments");
        }

        if (request.actualDeliveryDate() != null
                && status != ShipmentStatus.DELIVERED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Shipment with an actual delivery date must be DELIVERED");
        }
    }

    private void validateDates(ShipmentRequest request) {
        if (request.shipmentDate() != null
                && request.estimatedDeliveryDate() != null
                && request.estimatedDeliveryDate()
                        .isBefore(request.shipmentDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estimated delivery date cannot be before shipment date");
        }

        if (request.shipmentDate() != null
                && request.actualDeliveryDate() != null
                && request.actualDeliveryDate()
                        .isBefore(request.shipmentDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Actual delivery date cannot be before shipment date");
        }
    }

    private void validateTrackingNumber(String value, Long currentId) {
        if (value == null || value.isBlank()) {
            return;
        }

        shipmentRepository.findByTrackingNumber(value.trim())
                .filter(existing -> currentId == null
                        || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Tracking number already exists");
                });
    }

    private void synchronizeOrderStatus(ExportOrder order) {
        List<Shipment> shipments =
                shipmentRepository.findByExportOrder_Id(order.getId());

        if (shipments.isEmpty()) {
            return;
        }

        boolean allDelivered = shipments.stream()
                .allMatch(s -> s.getStatus() == ShipmentStatus.DELIVERED);

        if (allDelivered) {
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.saveAndFlush(order);
            return;
        }

        boolean anyInTransitOrCustoms = shipments.stream()
                .anyMatch(s -> s.getStatus() == ShipmentStatus.IN_TRANSIT
                        || s.getStatus() == ShipmentStatus.CUSTOMS);

        if (anyInTransitOrCustoms) {
            order.setStatus(OrderStatus.SHIPPED);
            orderRepository.saveAndFlush(order);
            return;
        }

        boolean anyPreparing = shipments.stream()
                .anyMatch(s -> s.getStatus() == ShipmentStatus.PREPARING);

        if (anyPreparing) {
            order.setStatus(OrderStatus.IN_PREPARATION);
            orderRepository.saveAndFlush(order);
        }
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
