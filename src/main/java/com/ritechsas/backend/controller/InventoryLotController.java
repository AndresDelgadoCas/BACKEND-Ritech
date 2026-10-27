
package com.ritechsas.backend.controller;

import com.ritechsas.backend.dto.InventoryLotRequest;
import com.ritechsas.backend.entity.InventoryLot;
import com.ritechsas.backend.entity.InventoryLot.LotStatus;
import com.ritechsas.backend.entity.Product;
import com.ritechsas.backend.entity.Producer;
import com.ritechsas.backend.repository.InventoryLotRepository;
import com.ritechsas.backend.repository.ProductRepository;
import com.ritechsas.backend.repository.ProducerRepository;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/inventory-lots")
public class InventoryLotController {

    private final InventoryLotRepository inventoryLotRepository;
    private final ProductRepository productRepository;
    private final ProducerRepository producerRepository;

    public InventoryLotController(
            InventoryLotRepository inventoryLotRepository,
            ProductRepository productRepository,
            ProducerRepository producerRepository) {
        this.inventoryLotRepository = inventoryLotRepository;
        this.productRepository = productRepository;
        this.producerRepository = producerRepository;
    }

    @GetMapping
    public List<InventoryLot> getAllInventoryLots() {
        return inventoryLotRepository.findAll();
    }

    @GetMapping("/{id}")
    public InventoryLot getInventoryLotById(@PathVariable Long id) {
        return inventoryLotRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Inventory lot not found"
            ));
    }

    @GetMapping("/status/{status}")
    public List<InventoryLot> getInventoryLotsByStatus(
            @PathVariable LotStatus status) {
        return inventoryLotRepository.findByStatus(status);
    }

    @PostMapping
    public ResponseEntity<InventoryLot> createInventoryLot(
            @Valid @RequestBody InventoryLotRequest request) {

        if (inventoryLotRepository.existsByLotCode(request.lotCode())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Lot code already exists"
            );
        }

        InventoryLot inventoryLot = new InventoryLot();
        copyRequestToInventoryLot(request, inventoryLot);

        InventoryLot savedLot = inventoryLotRepository.save(inventoryLot);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedLot);
    }

    @PutMapping("/{id}")
    public InventoryLot updateInventoryLot(
            @PathVariable Long id,
            @Valid @RequestBody InventoryLotRequest request) {

        InventoryLot inventoryLot = inventoryLotRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Inventory lot not found"
            ));

        if (!inventoryLot.getLotCode().equals(request.lotCode())
                && inventoryLotRepository.existsByLotCode(request.lotCode())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Lot code already exists"
            );
        }

        copyRequestToInventoryLot(request, inventoryLot);

        return inventoryLotRepository.save(inventoryLot);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventoryLot(@PathVariable Long id) {
        InventoryLot inventoryLot = inventoryLotRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Inventory lot not found"
            ));

        inventoryLotRepository.delete(inventoryLot);

        return ResponseEntity.noContent().build();
    }

    private void copyRequestToInventoryLot(
            InventoryLotRequest request,
            InventoryLot inventoryLot) {

        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Product not found"
            ));

        Producer producer = producerRepository.findById(request.producerId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Producer not found"
            ));

        inventoryLot.setProduct(product);
        inventoryLot.setProducer(producer);
        inventoryLot.setLotCode(request.lotCode());
        inventoryLot.setQuantity(request.quantity());
        inventoryLot.setHarvestDate(request.harvestDate());

        inventoryLot.setStatus(
            request.status() == null
                ? LotStatus.AVAILABLE
                : request.status()
        );
    }
}
