
package com.ritechsas.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExportOrderItemRequest(

    @NotNull(message = "Export order ID is required")
    @Positive(message = "Export order ID must be positive")
    Long exportOrderId,

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    Long productId,

    @NotNull(message = "Inventory lot ID is required")
    @Positive(message = "Inventory lot ID must be positive")
    Long inventoryLotId,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    @Digits(integer = 9, fraction = 3,
            message = "Quantity supports up to 9 integer digits and 3 decimals")
    BigDecimal quantityKg,

    @NotNull(message = "Sale price is required")
    @DecimalMin(value = "0.0", inclusive = true,
            message = "Sale price cannot be negative")
    @Digits(integer = 8, fraction = 4,
            message = "Sale price supports up to 8 integer digits and 4 decimals")
    BigDecimal salePriceUsdPerKg

) {}

