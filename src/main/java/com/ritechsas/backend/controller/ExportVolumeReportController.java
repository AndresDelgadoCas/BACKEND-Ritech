
package com.ritechsas.backend.controller;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ritechsas.backend.entity.ExportOrder;
import com.ritechsas.backend.entity.ExportOrderItem;
import com.ritechsas.backend.repository.ExportOrderItemRepository;
import com.ritechsas.backend.repository.ExportOrderRepository;

@RestController
@RequestMapping("/api/reports/exported-volume")
public class ExportVolumeReportController {

    private final ExportOrderRepository exportOrderRepository;
    private final ExportOrderItemRepository exportOrderItemRepository;

    public ExportVolumeReportController(
            ExportOrderRepository exportOrderRepository,
            ExportOrderItemRepository exportOrderItemRepository) {
        this.exportOrderRepository = exportOrderRepository;
        this.exportOrderItemRepository = exportOrderItemRepository;
    }

    @GetMapping("/monthly")
    public List<Map<String, Object>> getMonthlyExportedVolume() {

        Map<YearMonth, BigDecimal> monthlyVolumes = new TreeMap<>();

        List<ExportOrder.OrderStatus> exportedStatuses = List.of(
                ExportOrder.OrderStatus.SHIPPED,
                ExportOrder.OrderStatus.DELIVERED
        );

        for (ExportOrder.OrderStatus status : exportedStatuses) {

            List<ExportOrder> orders =
                    exportOrderRepository.findByStatus(status);

            for (ExportOrder order : orders) {

                if (order.getOrderDate() == null) {
                    continue;
                }

                YearMonth month = YearMonth.from(order.getOrderDate());

                List<ExportOrderItem> items =
                        exportOrderItemRepository
                                .findByExportOrder_Id(order.getId());

                BigDecimal orderVolume = BigDecimal.ZERO;

                for (ExportOrderItem item : items) {
                    if (item.getQuantityKg() != null) {
                        orderVolume = orderVolume.add(
                                item.getQuantityKg());
                    }
                }

                monthlyVolumes.merge(
                        month,
                        orderVolume,
                        BigDecimal::add
                );
            }
        }

        List<Map<String, Object>> response = new ArrayList<>();

        for (Map.Entry<YearMonth, BigDecimal> entry
                : monthlyVolumes.entrySet()) {

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("month", entry.getKey().toString());
            row.put("exportedVolumeKg", entry.getValue());

            response.add(row);
        }

        return response;
    }
}
