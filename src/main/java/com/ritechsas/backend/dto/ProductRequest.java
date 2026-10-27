
package com.ritechsas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductRequest(

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    String name,

    @NotBlank(message = "Product type is required")
    @Pattern(
        regexp = "COFFEE|COCOA",
        message = "Product type must be COFFEE or COCOA"
    )
    String productType,

    String description,

    @Size(max = 30, message = "Unit of measure cannot exceed 30 characters")
    String unitOfMeasure

) {
}
