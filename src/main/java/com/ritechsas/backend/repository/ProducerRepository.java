package com.ritechsas.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ritechsas.backend.entity.Producer;

public interface ProducerRepository extends JpaRepository<Producer, Long> {
    Optional<Producer> findByUserId(Long userId);
    List<Producer> findByUserIdIsNull();
}
