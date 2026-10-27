
package com.ritechsas.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProducerRequest(

    @NotBlank(message = "Producer name is required")
    @Size(max = 150, message = "Producer name cannot exceed 150 characters")
    String name,

    @Size(max = 150, message = "Contact cannot exceed 150 characters")
    String contact,

    @Size(max = 30, message = "Phone cannot exceed 30 characters")
    String phone,

    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    String email,

    @NotBlank(message = "Product type is required")
    @Pattern(
        regexp = "COFFEE|COCOA|BOTH",
        message = "Product type must be COFFEE, COCOA or BOTH"
    )
    String productType

) {
}
