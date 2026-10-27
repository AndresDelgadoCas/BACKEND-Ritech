
package com.ritechsas.backend.dto;

import java.time.LocalDate;

import com.ritechsas.backend.entity.Certification.CertificationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CertificationRequest(

    @NotNull(message = "Inventory lot ID is required")
    Long inventoryLotId,

    @NotBlank(message = "Certification type is required")
    @Size(max = 100, message = "Certification type cannot exceed 100 characters")
    String certificationType,

    @NotBlank(message = "Certificate code is required")
    @Size(max = 100, message = "Certificate code cannot exceed 100 characters")
    String certificateCode,

    @Size(max = 150, message = "Issuing organization cannot exceed 150 characters")
    String issuingOrganization,

    LocalDate issueDate,

    LocalDate expirationDate,

    CertificationStatus status

) {
}
