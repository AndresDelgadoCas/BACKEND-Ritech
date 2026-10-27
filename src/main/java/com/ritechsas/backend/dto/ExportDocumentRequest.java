
package com.ritechsas.backend.dto;

import com.ritechsas.backend.entity.ExportDocument.DocumentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExportDocumentRequest(

    @NotNull(message = "Export order ID is required")
    Long exportOrderId,

    @NotBlank(message = "Document type is required")
    @Size(max = 100, message = "Document type cannot exceed 100 characters")
    String documentType,

    @Size(max = 100, message = "Document number cannot exceed 100 characters")
    String documentNumber,

    @Size(max = 500, message = "File URL cannot exceed 500 characters")
    String fileUrl,

    DocumentStatus status

) {
}

