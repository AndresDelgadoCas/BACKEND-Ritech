package com.ritechsas.backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ritechsas.backend.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findFirstByBuyerUserIdAndStatus(Long userId, Cart.CartStatus status);
}
