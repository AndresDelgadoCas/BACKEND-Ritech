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

import com.ritechsas.backend.dto.PurchaseRequest;
import com.ritechsas.backend.entity.InventoryLot;
import com.ritechsas.backend.entity.Producer;
import com.ritechsas.backend.entity.Product;
import com.ritechsas.backend.entity.Purchase;
import com.ritechsas.backend.repository.InventoryLotRepository;
import com.ritechsas.backend.repository.ProducerRepository;
import com.ritechsas.backend.repository.ProductRepository;
import com.ritechsas.backend.repository.PurchaseRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseRepository purchaseRepository;
    private final ProducerRepository producerRepository;
    private final ProductRepository productRepository;
    private final InventoryLotRepository inventoryLotRepository;

    public PurchaseController(
            PurchaseRepository purchaseRepository,
            ProducerRepository producerRepository,
            ProductRepository productRepository,
            InventoryLotRepository inventoryLotRepository) {
        this.purchaseRepository = purchaseRepository;
        this.producerRepository = producerRepository;
        this.productRepository = productRepository;
        this.inventoryLotRepository = inventoryLotRepository;
    }

    @GetMapping
    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    @GetMapping("/{id}")
    public Purchase getPurchaseById(@PathVariable Long id) {
        return purchaseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Purchase not found"));
    }

    @GetMapping("/producer/{producerId}")
    public List<Purchase> getPurchasesByProducer(
            @PathVariable Long producerId) {
        if (!producerRepository.existsById(producerId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Producer not found");
        }
        return purchaseRepository.findByProducerId(producerId);
    }

    @GetMapping("/product/{productId}")
    public List<Purchase> getPurchasesByProduct(
            @PathVariable Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Product not found");
        }
        return purchaseRepository.findByProductId(productId);
    }

    @GetMapping("/inventory-lot/{inventoryLotId}")
    public List<Purchase> getPurchasesByInventoryLot(
            @PathVariable Long inventoryLotId) {
        if (!inventoryLotRepository.existsById(inventoryLotId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Inventory lot not found");
        }
        return purchaseRepository.findByInventoryLotId(inventoryLotId);
    }

    @PostMapping
    public ResponseEntity<Purchase> createPurchase(
            @Valid @RequestBody PurchaseRequest request) {
        Purchase purchase = new Purchase();
        copyRequest(purchase, request);
        Purchase saved = purchaseRepository.save(purchase);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Purchase updatePurchase(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequest request) {
        Purchase purchase = purchaseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Purchase not found"));
        copyRequest(purchase, request);
        return purchaseRepository.save(purchase);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long id) {
        Purchase purchase = purchaseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Purchase not found"));
        purchaseRepository.delete(purchase);
        return ResponseEntity.noContent().build();
    }

    private void copyRequest(
            Purchase purchase, PurchaseRequest request) {

        Producer producer = producerRepository.findById(request.producerId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Producer not found"));

        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Product not found"));

        InventoryLot lot = null;
        if (request.inventoryLotId() != null) {
            lot = inventoryLotRepository.findById(request.inventoryLotId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Inventory lot not found"));
        }

        purchase.setProducer(producer);
        purchase.setProduct(product);
        purchase.setInventoryLot(lot);
        purchase.setPurchaseDate(request.purchaseDate());
        purchase.setQuantityKg(request.quantityKg());
        purchase.setPurchasePriceUsdPerKg(
            request.purchasePriceUsdPerKg());
        purchase.setNotes(request.notes());
    }
}