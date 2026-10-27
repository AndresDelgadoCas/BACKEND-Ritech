
package com.ritechsas.backend.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ritechsas.backend.entity.InventoryLot;
import com.ritechsas.backend.repository.ExportOrderItemRepository;
import com.ritechsas.backend.repository.InventoryLotRepository;

@RestController
@RequestMapping("/api/reports")
public class StockReportController {

    private final InventoryLotRepository inventoryLotRepository;
    private final ExportOrderItemRepository exportOrderItemRepository;

    public StockReportController(
            InventoryLotRepository inventoryLotRepository,
            ExportOrderItemRepository exportOrderItemRepository) {
        this.inventoryLotRepository = inventoryLotRepository;
        this.exportOrderItemRepository = exportOrderItemRepository;
    }

    @GetMapping("/stock-by-product-type")
    public List<Map<String, Object>> getStockByProductType() {
        List<InventoryLot> availableLots =
                inventoryLotRepository.findByStatus(
                        InventoryLot.LotStatus.AVAILABLE);

        Map<String, BigDecimal> stockByProductType =
                new LinkedHashMap<>();

        for (InventoryLot lot : availableLots) {
            BigDecimal physicalQuantity = lot.getQuantity();

            BigDecimal committedQuantity =
                    exportOrderItemRepository
                            .sumCommittedQuantityByLotId(lot.getId());

            BigDecimal availableQuantity =
                    physicalQuantity.subtract(committedQuantity)
                            .max(BigDecimal.ZERO);

            String productType = lot.getProduct().getProductType();

            stockByProductType.merge(
                    productType,
                    availableQuantity,
                    BigDecimal::add);
        }

        List<Map<String, Object>> response = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry
                : stockByProductType.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productType", entry.getKey());
            item.put("availableStockKg", entry.getValue());
            response.add(item);
        }

        return response;
    }
}
