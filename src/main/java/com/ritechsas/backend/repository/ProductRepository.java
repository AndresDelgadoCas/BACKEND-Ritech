
package com.ritechsas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ritechsas.backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
