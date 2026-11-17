package com.ritechsas.backend.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ritechsas.backend.entity.SellerApplication;
public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {
    List<SellerApplication> findAllByOrderByCreatedAtDesc();
    List<SellerApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndStatus(Long userId, SellerApplication.Status status);
}
