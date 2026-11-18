package com.ritechsas.backend.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ritechsas.backend.entity.Favorite;
public interface FavoriteRepository extends JpaRepository<Favorite, Long> { List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId); boolean existsByUserIdAndListingId(Long userId, Long listingId); void deleteByUserIdAndListingId(Long userId, Long listingId); }
