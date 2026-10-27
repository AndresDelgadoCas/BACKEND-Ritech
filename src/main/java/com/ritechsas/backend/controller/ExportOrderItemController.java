
package com.ritechsas.backend.controller;

import java.math.BigDecimal;
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

import com.ritechsas.backend.dto.ExportOrderItemRequest;
import com.ritechsas.backend.entity.ExportOrder;
import com.ritechsas.backend.entity.ExportOrder.OrderStatus;
import com.ritechsas.backend.entity.ExportOrderItem;
import com.ritechsas.backend.entity.InventoryLot;
import com.ritechsas.backend.entity.InventoryLot.LotStatus;
import com.ritechsas.backend.entity.Product;
import com.ritechsas.backend.repository.ExportOrderItemRepository;
import com.ritechsas.backend.repository.ExportOrderRepository;
import com.ritechsas.backend.repository.InventoryLotRepository;
import com.ritechsas.backend.repository.ProductRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/export-order-items")
public class ExportOrderItemController {

    private final ExportOrderItemRepository itemRepository;
    private final ExportOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryLotRepository lotRepository;

    public ExportOrderItemController(
            ExportOrderItemRepository itemRepository,
            ExportOrderRepository orderRepository,
            ProductRepository productRepository,
            InventoryLotRepository lotRepository) {
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.lotRepository = lotRepository;
    }

    @GetMapping
    public List<ExportOrderItem> getAll(
            @RequestParam(required = false) Long exportOrderId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long inventoryLotId) {

        if (exportOrderId != null) {
            return itemRepository.findByExportOrder_Id(exportOrderId);
        }

        if (productId != null) {
            return itemRepository.findByProduct_Id(productId);
        }

        if (inventoryLotId != null) {
            return itemRepository.findByInventoryLot_Id(inventoryLotId);
        }

        return itemRepository.findAll();
    }

    @GetMapping("/{id}")
    public ExportOrderItem getById(@PathVariable Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Export order item not found"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ExportOrderItem> create(
            @Valid @RequestBody ExportOrderItemRequest request) {

        ExportOrder order = orderRepository.findById(request.exportOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found"));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));

        InventoryLot lot = lotRepository.findById(request.inventoryLotId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inventory lot not found"));

        validateProductAndLot(product, lot);
        validateAvailableQuantity(lot, request.quantityKg(), null);

        ExportOrderItem item = new ExportOrderItem();
        item.setExportOrder(order);
        item.setProduct(product);
        item.setInventoryLot(lot);
        item.setQuantityKg(request.quantityKg());
        item.setSalePriceUsdPerKg(request.salePriceUsdPerKg());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemRepository.saveAndFlush(item));
    }

    @PutMapping("/{id}")
    @Transactional
    public ExportOrderItem update(
            @PathVariable Long id,
            @Valid @RequestBody ExportOrderItemRequest request) {

        ExportOrderItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Export order item not found"));

        ExportOrder order = orderRepository.findById(request.exportOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found"));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));

        InventoryLot lot = lotRepository.findById(request.inventoryLotId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inventory lot not found"));

        validateProductAndLot(product, lot);
        validateAvailableQuantity(lot, request.quantityKg(), item);

        item.setExportOrder(order);
        item.setProduct(product);
        item.setInventoryLot(lot);
        item.setQuantityKg(request.quantityKg());
        item.setSalePriceUsdPerKg(request.salePriceUsdPerKg());

        return itemRepository.saveAndFlush(item);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ExportOrderItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Export order item not found"));

        itemRepository.delete(item);
        itemRepository.flush();

        return ResponseEntity.noContent().build();
    }

    private void validateProductAndLot(Product product, InventoryLot lot) {
        if (!lot.getProduct().getId().equals(product.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inventory lot does not belong to the selected product");
        }

        if (lot.getStatus() != LotStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inventory lot is not available for export");
        }
    }

    private void validateAvailableQuantity(
            InventoryLot lot,
            BigDecimal requestedQuantity,
            ExportOrderItem itemBeingUpdated) {

        BigDecimal committedQuantity =
                itemRepository.sumCommittedQuantityByLotId(lot.getId());

        BigDecimal quantityToRestore = BigDecimal.ZERO;

        if (itemBeingUpdated != null
                && itemBeingUpdated.getInventoryLot().getId()
                        .equals(lot.getId())
                && countsAsCommitted(
                        itemBeingUpdated.getExportOrder().getStatus())) {

            quantityToRestore = itemBeingUpdated.getQuantityKg();
        }

        BigDecimal availableQuantity = lot.getQuantity()
                .subtract(committedQuantity)
                .add(quantityToRestore);

        if (availableQuantity.compareTo(BigDecimal.ZERO) < 0) {
            availableQuantity = BigDecimal.ZERO;
        }

        if (requestedQuantity.compareTo(availableQuantity) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requested quantity exceeds the available inventory. "
                            + "Available: " + availableQuantity + " kg");
        }
    }

    private boolean countsAsCommitted(OrderStatus status) {
        return status != OrderStatus.DRAFT
                && status != OrderStatus.CANCELLED;
    }
}
