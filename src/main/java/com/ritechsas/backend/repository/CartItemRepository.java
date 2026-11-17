package com.ritechsas.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ritechsas.backend.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCart_Id(Long cartId);
    Optional<CartItem> findByIdAndCart_BuyerUserId(Long id, Long userId);
    void deleteByCart_Id(Long cartId);
}
