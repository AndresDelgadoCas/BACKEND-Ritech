
package com.ritechsas.backend.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ritechsas.backend.dto.ExportDocumentRequest;
import com.ritechsas.backend.entity.ExportDocument;
import com.ritechsas.backend.entity.ExportOrder;
import com.ritechsas.backend.repository.ExportDocumentRepository;
import com.ritechsas.backend.repository.ExportOrderRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/export-documents")
public class ExportDocumentController {

    private final ExportDocumentRepository documentRepository;
    private final ExportOrderRepository orderRepository;

    public ExportDocumentController(
            ExportDocumentRepository documentRepository,
            ExportOrderRepository orderRepository) {
        this.documentRepository = documentRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public List<ExportDocument> getAll(
            @RequestParam(required = false) Long exportOrderId,
            @RequestParam(required = false) ExportDocument.DocumentStatus status) {

        if (exportOrderId != null) {
            if (!orderRepository.existsById(exportOrderId)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found");
            }
            return documentRepository.findByExportOrder_Id(exportOrderId)
                    .stream()
                    .filter(document -> status == null
                            || document.getStatus() == status)
                    .toList();
        }

        if (status != null) {
            return documentRepository.findByStatus(status);
        }

        return documentRepository.findAll();
    }

    @GetMapping("/{id}")
    public ExportDocument getById(@PathVariable Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export document not found"));
    }

    @PostMapping
    public ResponseEntity<ExportDocument> create(
            @Valid @RequestBody ExportDocumentRequest request) {

        ExportOrder order = orderRepository.findById(request.exportOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found"));

        ExportDocument document = new ExportDocument();
        document.setExportOrder(order);
        document.setDocumentType(request.documentType().trim());
        document.setDocumentNumber(clean(request.documentNumber()));
        document.setFileUrl(clean(request.fileUrl()));
        document.setStatus(request.status() == null
                ? ExportDocument.DocumentStatus.PENDING
                : request.status());

        if (document.getStatus() != ExportDocument.DocumentStatus.PENDING) {
            document.setGeneratedAt(LocalDateTime.now());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentRepository.saveAndFlush(document));
    }

    @PutMapping("/{id}")
    public ExportDocument update(
            @PathVariable Long id,
            @Valid @RequestBody ExportDocumentRequest request) {

        ExportDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export document not found"));

        ExportOrder order = orderRepository.findById(request.exportOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export order not found"));

        document.setExportOrder(order);
        document.setDocumentType(request.documentType().trim());
        document.setDocumentNumber(clean(request.documentNumber()));
        document.setFileUrl(clean(request.fileUrl()));

        ExportDocument.DocumentStatus newStatus = request.status() == null
                ? document.getStatus()
                : request.status();

        document.setStatus(newStatus);

        if (newStatus != ExportDocument.DocumentStatus.PENDING
                && document.getGeneratedAt() == null) {
            document.setGeneratedAt(LocalDateTime.now());
        }

        if (newStatus == ExportDocument.DocumentStatus.PENDING) {
            document.setGeneratedAt(null);
        }

        return documentRepository.saveAndFlush(document);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ExportDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Export document not found"));

        documentRepository.delete(document);
        documentRepository.flush();

        return ResponseEntity.noContent().build();
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
