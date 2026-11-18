package com.ritechsas.backend.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ritechsas.backend.entity.SellerStatusHistory;
public interface SellerStatusHistoryRepository extends JpaRepository<SellerStatusHistory, Long> { List<SellerStatusHistory> findByProducerIdOrderByCreatedAtDesc(Long producerId); }
