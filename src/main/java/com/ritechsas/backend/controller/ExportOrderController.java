
package com.ritechsas.backend.controller;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
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

import com.ritechsas.backend.dto.ExportOrderRequest;
import com.ritechsas.backend.entity.Client;
import com.ritechsas.backend.entity.ExportOrder;
import com.ritechsas.backend.entity.ExportOrder.OrderStatus;
import com.ritechsas.backend.entity.Shipment;
import com.ritechsas.backend.entity.Shipment.ShipmentStatus;
import com.ritechsas.backend.repository.ClientRepository;
import com.ritechsas.backend.repository.ExportOrderRepository;
import com.ritechsas.backend.repository.ShipmentRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/export-orders")
public class ExportOrderController {

    private final ExportOrderRepository exportOrderRepository;
    private final ClientRepository clientRepository;
    private final ShipmentRepository shipmentRepository;

    public ExportOrderController(
            ExportOrderRepository exportOrderRepository,
            ClientRepository clientRepository,
            ShipmentRepository shipmentRepository) {
        this.exportOrderRepository = exportOrderRepository;
        this.clientRepository = clientRepository;
        this.shipmentRepository = shipmentRepository;
    }

    @GetMapping
    public List<ExportOrder> getAllExportOrders(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) OrderStatus status) {

        if (clientId != null && !clientRepository.existsById(clientId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Client not found");
        }

        if (clientId != null && status != null) {
            return exportOrderRepository.findByClientId(clientId).stream()
                    .filter(order -> order.getStatus() == status)
                    .toList();
        }

        if (clientId != null) {
            return exportOrderRepository.findByClientId(clientId);
        }

        if (status != null) {
            return exportOrderRepository.findByStatus(status);
        }

        return exportOrderRepository.findAll();
    }

    @GetMapping("/{id}")
    public ExportOrder getExportOrderById(@PathVariable Long id) {
        return exportOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found"));
    }

    @PostMapping
    public ResponseEntity<ExportOrder> createExportOrder(
            @Valid @RequestBody ExportOrderRequest request) {

        if (exportOrderRepository.existsByOrderCode(request.orderCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Order code already exists");
        }

        ExportOrder order = new ExportOrder();
        copyRequest(order, request);

        try {
            ExportOrder saved = exportOrderRepository.saveAndFlush(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order code or related data violates a database constraint");
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ExportOrder updateExportOrder(
            @PathVariable Long id,
            @Valid @RequestBody ExportOrderRequest request) {

        ExportOrder order = exportOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found"));

        exportOrderRepository.findByOrderCode(request.orderCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Order code already exists");
                });

        copyRequest(order, request);
        synchronizeStatusFromShipments(order);

        try {
            return exportOrderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order code or related data violates a database constraint");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExportOrder(@PathVariable Long id) {
        ExportOrder order = exportOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found"));

        try {
            exportOrderRepository.delete(order);
            exportOrderRepository.flush();
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order cannot be deleted because it is referenced by other records");
        }
    }

    private void copyRequest(
            ExportOrder order, ExportOrderRequest request) {

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Client not found"));

        order.setOrderCode(request.orderCode().trim());
        order.setClient(client);
        order.setDestinationCountry(request.destinationCountry().trim());
        order.setDestinationAddress(request.destinationAddress());
        order.setOrderDate(request.orderDate());
        order.setStatus(request.status() == null
                ? OrderStatus.DRAFT
                : request.status());
        order.setNotes(request.notes());
    }

    private void synchronizeStatusFromShipments(ExportOrder order) {
        List<Shipment> shipments =
                shipmentRepository.findByExportOrder_Id(order.getId());

        if (shipments.isEmpty()) {
            return;
        }

        boolean allDelivered = shipments.stream()
                .allMatch(shipment ->
                        shipment.getStatus() == ShipmentStatus.DELIVERED);

        if (allDelivered) {
            order.setStatus(OrderStatus.DELIVERED);
            return;
        }

        boolean anyInTransitOrCustoms = shipments.stream()
                .anyMatch(shipment ->
                        shipment.getStatus() == ShipmentStatus.IN_TRANSIT
                        || shipment.getStatus() == ShipmentStatus.CUSTOMS);

        if (anyInTransitOrCustoms) {
            order.setStatus(OrderStatus.SHIPPED);
            return;
        }

        boolean anyPreparing = shipments.stream()
                .anyMatch(shipment ->
                        shipment.getStatus() == ShipmentStatus.PREPARING);

        if (anyPreparing) {
            order.setStatus(OrderStatus.IN_PREPARATION);
        }
    }
}
