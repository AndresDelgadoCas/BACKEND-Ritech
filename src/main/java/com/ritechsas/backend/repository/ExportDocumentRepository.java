
package com.ritechsas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ritechsas.backend.entity.ExportDocument;
import com.ritechsas.backend.entity.ExportDocument.DocumentStatus;

public interface ExportDocumentRepository
        extends JpaRepository<ExportDocument, Long> {

    List<ExportDocument> findByExportOrder_Id(Long orderId);

    List<ExportDocument> findByStatus(DocumentStatus status);
}
