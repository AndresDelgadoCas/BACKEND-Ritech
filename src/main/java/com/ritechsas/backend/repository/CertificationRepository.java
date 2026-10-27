
package com.ritechsas.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ritechsas.backend.entity.Certification;
import com.ritechsas.backend.entity.Certification.CertificationStatus;

public interface CertificationRepository
        extends JpaRepository<Certification, Long> {

    List<Certification> findByStatus(CertificationStatus status);

    List<Certification> findByInventoryLotId(Long inventoryLotId);

    boolean existsByCertificateCode(String certificateCode);
}
