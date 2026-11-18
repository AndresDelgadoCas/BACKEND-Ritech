package com.ritechsas.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ritechsas.backend.entity.SupportTicket;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByRequesterUserIdOrderByCreatedAtDesc(Long requesterUserId);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    List<SupportTicket> findByStatusAndEscalatedToOrderByCreatedAtDesc(String status, Long escalatedTo);
}
