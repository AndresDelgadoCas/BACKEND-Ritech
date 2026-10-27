
package com.ritechsas.backend.controller;

import com.ritechsas.backend.dto.CertificationRequest;
import com.ritechsas.backend.entity.Certification;
import com.ritechsas.backend.entity.Certification.CertificationStatus;
import com.ritechsas.backend.entity.InventoryLot;
import com.ritechsas.backend.repository.CertificationRepository;
import com.ritechsas.backend.repository.InventoryLotRepository;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {

    private final CertificationRepository certificationRepository;
    private final InventoryLotRepository inventoryLotRepository;

    public CertificationController(
            CertificationRepository certificationRepository,
            InventoryLotRepository inventoryLotRepository) {
        this.certificationRepository = certificationRepository;
        this.inventoryLotRepository = inventoryLotRepository;
    }

    @GetMapping
    public List<Certification> getAllCertifications() {
        return certificationRepository.findAll();
    }

    @GetMapping("/{id}")
    public Certification getCertificationById(@PathVariable Long id) {
        return certificationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Certification not found"
            ));
    }

    @GetMapping("/status/{status}")
    public List<Certification> getCertificationsByStatus(
            @PathVariable CertificationStatus status) {
        return certificationRepository.findByStatus(status);
    }

    @GetMapping("/inventory-lot/{inventoryLotId}")
    public List<Certification> getCertificationsByInventoryLot(
            @PathVariable Long inventoryLotId) {

        if (!inventoryLotRepository.existsById(inventoryLotId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Inventory lot not found"
            );
        }

        return certificationRepository.findByInventoryLotId(inventoryLotId);
    }

    @PostMapping
    public ResponseEntity<Certification> createCertification(
            @Valid @RequestBody CertificationRequest request) {

        if (certificationRepository.existsByCertificateCode(
                request.certificateCode())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Certificate code already exists"
            );
        }

        Certification certification = new Certification();
        copyRequestToCertification(request, certification);

        Certification savedCertification =
            certificationRepository.save(certification);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(savedCertification);
    }

    @PutMapping("/{id}")
    public Certification updateCertification(
            @PathVariable Long id,
            @Valid @RequestBody CertificationRequest request) {

        Certification certification = certificationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Certification not found"
            ));

        if (!certification.getCertificateCode().equals(
                request.certificateCode())
                && certificationRepository.existsByCertificateCode(
                    request.certificateCode())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Certificate code already exists"
            );
        }

        copyRequestToCertification(request, certification);

        return certificationRepository.save(certification);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCertification(
            @PathVariable Long id) {

        Certification certification = certificationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Certification not found"
            ));

        certificationRepository.delete(certification);

        return ResponseEntity.noContent().build();
    }

    private void copyRequestToCertification(
            CertificationRequest request,
            Certification certification) {

        InventoryLot inventoryLot = inventoryLotRepository
            .findById(request.inventoryLotId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Inventory lot not found"
            ));

        certification.setInventoryLot(inventoryLot);
        certification.setCertificationType(request.certificationType());
        certification.setCertificateCode(request.certificateCode());
        certification.setIssuingOrganization(
            request.issuingOrganization());
        certification.setIssueDate(request.issueDate());
        certification.setExpirationDate(request.expirationDate());

        certification.setStatus(
            request.status() == null
                ? CertificationStatus.PENDING
                : request.status()
        );
    }
}
