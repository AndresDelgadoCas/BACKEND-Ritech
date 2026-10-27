
package com.ritechsas.backend.dto;

import com.ritechsas.backend.entity.InventoryLot.LotStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryLotRequest(

    @NotNull(message = "Product ID is required")
    Long productId,

    @NotNull(message = "Producer ID is required")
    Long producerId,

    @NotBlank(message = "Lot code is required")
    @Size(max = 80, message = "Lot code cannot exceed 80 characters")
    String lotCode,

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.000", message = "Quantity cannot be negative")
    BigDecimal quantity,

    LocalDate harvestDate,

    LotStatus status

) {
}
