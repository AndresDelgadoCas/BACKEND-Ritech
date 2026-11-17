package com.ritechsas.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ritechsas.backend.entity.ProductListing;

public interface ProductListingRepository extends JpaRepository<ProductListing, Long> {
    List<ProductListing> findByStatusOrderByCreatedAtDesc(ProductListing.ListingStatus status);
    List<ProductListing> findByProducer_UserIdOrderByCreatedAtDesc(Long userId);
    Optional<ProductListing> findByIdAndProducer_UserId(Long id, Long userId);
    List<ProductListing> findByInventoryLot_Id(Long lotId);
    boolean existsByInventoryLot_Id(Long lotId);
}
