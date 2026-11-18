package com.ritechsas.backend.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ritechsas.backend.entity.SupportMessage;
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> { List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId); }
