
package com.ritechsas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ritechsas.backend.entity.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
