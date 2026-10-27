package com.ritechsas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PurchaseRequest(

    @NotNull(message = "Producer ID is required")
    Long producerId,

    @NotNull(message = "Product ID is required")
    Long productId,

    Long inventoryLotId,

    @NotNull(message = "Purchase date is required")
    LocalDate purchaseDate,

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    BigDecimal quantityKg,

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.0000", message = "Purchase price cannot be negative")
    BigDecimal purchasePriceUsdPerKg,

    @Size(max = 10000, message = "Notes cannot exceed 10000 characters")
    String notes

) {
}