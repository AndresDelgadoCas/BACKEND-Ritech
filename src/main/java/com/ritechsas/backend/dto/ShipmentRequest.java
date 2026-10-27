
package com.ritechsas.backend.dto;

import java.time.LocalDate;

import com.ritechsas.backend.entity.Shipment.ShipmentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ShipmentRequest(

    @NotNull(message = "Export order ID is required")
    Long exportOrderId,

    @Size(max = 120, message = "Tracking number cannot exceed 120 characters")
    String trackingNumber,

    @Size(max = 150, message = "Carrier cannot exceed 150 characters")
    String carrier,

    LocalDate shipmentDate,

    LocalDate estimatedDeliveryDate,

    LocalDate actualDeliveryDate,

    ShipmentStatus status,

    @Size(max = 10000, message = "Notes cannot exceed 10000 characters")
    String notes

) {
}

