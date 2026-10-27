
package com.ritechsas.backend.dto;

import java.time.LocalDate;

import com.ritechsas.backend.entity.ExportOrder.OrderStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExportOrderRequest(

    @NotBlank(message = "Order code is required")
    @Size(max = 80, message = "Order code cannot exceed 80 characters")
    String orderCode,

    @NotNull(message = "Client ID is required")
    Long clientId,

    @NotBlank(message = "Destination country is required")
    @Size(max = 100, message = "Destination country cannot exceed 100 characters")
    String destinationCountry,

    @Size(max = 255, message = "Destination address cannot exceed 255 characters")
    String destinationAddress,

    @NotNull(message = "Order date is required")
    LocalDate orderDate,

    OrderStatus status,

    @Size(max = 10000, message = "Notes cannot exceed 10000 characters")
    String notes

) {
}
