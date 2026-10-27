
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

import com.ritechsas.backend.dto.ClientRequest;
import com.ritechsas.backend.entity.Client;
import com.ritechsas.backend.entity.ExportOrder;
import com.ritechsas.backend.repository.ClientRepository;
import com.ritechsas.backend.repository.ExportOrderRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository clientRepository;
    private final ExportOrderRepository exportOrderRepository;

    public ClientController(
            ClientRepository clientRepository,
            ExportOrderRepository exportOrderRepository) {
        this.clientRepository = clientRepository;
        this.exportOrderRepository = exportOrderRepository;
    }

    @GetMapping
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    @GetMapping("/{id}")
    public Client getClientById(@PathVariable Long id) {
        return clientRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Client not found"
            ));
    }

    @GetMapping("/{id}/orders")
    public List<ExportOrder> getClientOrders(@PathVariable Long id) {

        clientRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Client not found"
            ));

        return exportOrderRepository.findByClientId(id);
    }

    @PostMapping
    public ResponseEntity<Client> createClient(
            @Valid @RequestBody ClientRequest request) {

        Client client = new Client();
        copyRequestToClient(request, client);

        Client savedClient = clientRepository.save(client);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedClient);
    }

    @PutMapping("/{id}")
    public Client updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request) {

        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Client not found"
            ));

        copyRequestToClient(request, client);

        return clientRepository.save(client);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Client not found"
            ));

        clientRepository.delete(client);

        return ResponseEntity.noContent().build();
    }

    private void copyRequestToClient(
            ClientRequest request,
            Client client) {

        client.setCompanyName(request.companyName());
        client.setContactName(request.contactName());
        client.setCountry(request.country());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setAddress(request.address());
    }
}
