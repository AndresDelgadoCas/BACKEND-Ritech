
package com.ritechsas.backend.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ritechsas.backend.entity.Certification;
import com.ritechsas.backend.entity.ExportOrder;
import com.ritechsas.backend.entity.ExportOrderItem;
import com.ritechsas.backend.entity.InventoryLot;
import com.ritechsas.backend.repository.CertificationRepository;
import com.ritechsas.backend.repository.ExportDocumentRepository;
import com.ritechsas.backend.repository.ExportOrderItemRepository;
import com.ritechsas.backend.repository.ExportOrderRepository;
import com.ritechsas.backend.repository.InventoryLotRepository;
import com.ritechsas.backend.repository.ShipmentRepository;

@RestController
@RequestMapping("/api/reports/traceability")
public class TraceabilityReportController {

    private final ExportOrderRepository exportOrderRepository;
    private final ExportOrderItemRepository exportOrderItemRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final CertificationRepository certificationRepository;
    private final ExportDocumentRepository exportDocumentRepository;
    private final ShipmentRepository shipmentRepository;

    public TraceabilityReportController(
            ExportOrderRepository exportOrderRepository,
            ExportOrderItemRepository exportOrderItemRepository,
            InventoryLotRepository inventoryLotRepository,
            CertificationRepository certificationRepository,
            ExportDocumentRepository exportDocumentRepository,
            ShipmentRepository shipmentRepository) {

        this.exportOrderRepository = exportOrderRepository;
        this.exportOrderItemRepository = exportOrderItemRepository;
        this.inventoryLotRepository = inventoryLotRepository;
        this.certificationRepository = certificationRepository;
        this.exportDocumentRepository = exportDocumentRepository;
        this.shipmentRepository = shipmentRepository;
    }

    @GetMapping("/{orderId}")
    public Map<String, Object> getTraceabilityReport(
            @PathVariable Long orderId) {

        ExportOrder order = exportOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Export order not found: " + orderId));

        Map<String, Object> report = new LinkedHashMap<>();

        report.put("reportType", "EU_EXPORT_TRACEABILITY");
        report.put("order", buildOrderData(order));

        List<Map<String, Object>> items = new ArrayList<>();

        List<ExportOrderItem> orderItems =
                exportOrderItemRepository.findByExportOrder_Id(orderId);

        for (ExportOrderItem item : orderItems) {
            Map<String, Object> itemData = new LinkedHashMap<>();

            itemData.put("itemId", item.getId());
            itemData.put("quantityKg", item.getQuantityKg());
            itemData.put("salePriceUsdPerKg",
                    item.getSalePriceUsdPerKg());
            itemData.put("totalAmountUsd", item.getTotalAmountUsd());

            if (item.getProduct() != null) {
                Map<String, Object> productData = new LinkedHashMap<>();
                productData.put("id", item.getProduct().getId());
                productData.put("name", item.getProduct().getName());
                productData.put(
                        "productType",
                        item.getProduct().getProductType());
                productData.put(
                        "unitOfMeasure",
                        item.getProduct().getUnitOfMeasure());

                itemData.put("product", productData);
            }

            InventoryLot lot = item.getInventoryLot();

            if (lot != null) {
                itemData.put("inventoryLot",
                        buildLotData(lot));
                itemData.put("certifications",
                        buildCertifications(lot.getId()));
            } else {
                itemData.put("inventoryLot", null);
                itemData.put("certifications", List.of());
            }

            items.add(itemData);
        }

        report.put("items", items);

        report.put(
                "exportDocumentIds",
                exportDocumentRepository.findByExportOrder_Id(orderId)
                        .stream()
                        .map(document -> document.getId())
                        .toList());

        report.put(
                "shipmentIds",
                shipmentRepository.findByExportOrder_Id(orderId)
                        .stream()
                        .map(shipment -> shipment.getId())
                        .toList());

        report.put("itemCount", items.size());

        return report;
    }

    private Map<String, Object> buildOrderData(ExportOrder order) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("id", order.getId());
        data.put("orderCode", order.getOrderCode());
        data.put("orderDate", order.getOrderDate());
        data.put("status", order.getStatus());
        data.put("destinationCountry", order.getDestinationCountry());
        data.put("destinationAddress", order.getDestinationAddress());

        if (order.getClient() != null) {
            Map<String, Object> client = new LinkedHashMap<>();

            client.put("id", order.getClient().getId());
            client.put("companyName", order.getClient().getCompanyName());
            client.put("country", order.getClient().getCountry());

            data.put("client", client);
        } else {
            data.put("client", null);
        }

        return data;
    }

    private Map<String, Object> buildLotData(InventoryLot lot) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("id", lot.getId());
        data.put("lotCode", lot.getLotCode());
        data.put("quantity", lot.getQuantity());
        data.put("harvestDate", lot.getHarvestDate());
        data.put("receivedAt", lot.getReceivedAt());
        data.put("status", lot.getStatus());

        if (lot.getProducer() != null) {
            data.put("producerId", lot.getProducer().getId());
        } else {
            data.put("producerId", null);
        }

        return data;
    }

    private List<Map<String, Object>> buildCertifications(
            Long inventoryLotId) {

        List<Map<String, Object>> result = new ArrayList<>();

        List<Certification> certifications =
                certificationRepository.findByInventoryLotId(
                        inventoryLotId);

        for (Certification certification : certifications) {
            Map<String, Object> data = new LinkedHashMap<>();

            data.put("id", certification.getId());
            data.put("certificationType",
                    certification.getCertificationType());
            data.put("certificateCode",
                    certification.getCertificateCode());
            data.put("issuingOrganization",
                    certification.getIssuingOrganization());
            data.put("issueDate", certification.getIssueDate());
            data.put("expirationDate",
                    certification.getExpirationDate());
            data.put("status", certification.getStatus());

            result.add(data);
        }

        return result;
    }
}
