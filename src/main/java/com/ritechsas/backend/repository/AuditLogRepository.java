package com.ritechsas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ritechsas.backend.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
