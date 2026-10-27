
package com.ritechsas.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ritechsas.backend.dto.ProducerRequest;
import com.ritechsas.backend.entity.Producer;
import com.ritechsas.backend.entity.Purchase;
import com.ritechsas.backend.repository.ProducerRepository;
import com.ritechsas.backend.repository.PurchaseRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/producers")
public class ProducerController {

    private final ProducerRepository producerRepository;
    private final PurchaseRepository purchaseRepository;

    public ProducerController(
            ProducerRepository producerRepository,
            PurchaseRepository purchaseRepository) {
        this.producerRepository = producerRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @GetMapping
    public List<Producer> getAllProducers() {
        return producerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Producer getProducerById(@PathVariable Long id) {
        return producerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Producer not found"
            ));
    }

    @GetMapping("/{id}/purchases")
    public List<Purchase> getProducerPurchases(@PathVariable Long id) {
        producerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Producer not found"
            ));

        return purchaseRepository.findByProducerId(id);
    }

    @PostMapping
    public ResponseEntity<Producer> createProducer(
            @Valid @RequestBody ProducerRequest request) {
        Producer producer = new Producer();
        copyRequestToProducer(request, producer);

        Producer savedProducer = producerRepository.save(producer);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedProducer);
    }

    @PutMapping("/{id}")
    public Producer updateProducer(
            @PathVariable Long id,
            @Valid @RequestBody ProducerRequest request) {
        Producer producer = producerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Producer not found"
            ));

        copyRequestToProducer(request, producer);

        return producerRepository.save(producer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProducer(@PathVariable Long id) {
        Producer producer = producerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Producer not found"
            ));

        producerRepository.delete(producer);

        return ResponseEntity.noContent().build();
    }

    private void copyRequestToProducer(
            ProducerRequest request,
            Producer producer) {
        producer.setName(request.name());
        producer.setContact(request.contact());
        producer.setPhone(request.phone());
        producer.setEmail(request.email());
        producer.setProductType(request.productType());
    }
}
