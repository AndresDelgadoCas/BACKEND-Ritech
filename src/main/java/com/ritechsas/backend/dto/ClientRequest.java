
package com.ritechsas.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(

    @NotBlank(message = "Company name is required")
    @Size(max = 180, message = "Company name cannot exceed 180 characters")
    String companyName,

    @NotBlank(message = "Contact name is required")
    @Size(max = 150, message = "Contact name cannot exceed 150 characters")
    String contactName,

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    String country,

    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    String email,

    @Size(max = 30, message = "Phone cannot exceed 30 characters")
    String phone,

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    String address

) {
}
