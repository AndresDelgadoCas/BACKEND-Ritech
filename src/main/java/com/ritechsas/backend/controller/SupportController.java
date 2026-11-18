package com.ritechsas.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ritechsas.backend.entity.SupportMessage;
import com.ritechsas.backend.entity.SupportTicket;
import com.ritechsas.backend.service.SupportService;
import com.ritechsas.backend.service.SupportService.*;

@RestController
@RequestMapping("/api/support")
public class SupportController {
    private final SupportService service;
    public SupportController(SupportService service) { this.service = service; }

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicket create(Authentication auth, @RequestBody TicketRequest request) { return service.create(auth.getName(), request); }

    @GetMapping("/tickets/mine")
    public List<SupportTicket> mine(Authentication auth) { return service.mine(auth.getName()); }

    @GetMapping("/management/tickets")
    public List<SupportTicket> all(Authentication auth) { return service.all(auth.getName()); }

    @GetMapping("/management/admins")
    public List<StaffOption> administrators(Authentication auth) { return service.administrators(auth.getName()); }

    @GetMapping("/admin/tickets/escalated")
    public List<SupportTicket> escalated(Authentication auth) { return service.escalatedForAdministrator(auth.getName()); }

    @PostMapping("/admin/tickets/{id}/decision")
    public SupportTicket review(Authentication auth, @PathVariable Long id, @RequestBody AdminDecisionRequest request) {
        return service.reviewEscalation(auth.getName(), id, request);
    }

    @GetMapping("/tickets/{id}/messages")
    public List<SupportMessage> messages(Authentication auth, @PathVariable Long id) { return service.messages(auth.getName(), id); }

    @PostMapping("/tickets/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportMessage reply(Authentication auth, @PathVariable Long id, @RequestBody MessageRequest request) {
        return service.reply(auth.getName(), id, request);
    }

    @PatchMapping("/management/tickets/{id}")
    public SupportTicket update(Authentication auth, @PathVariable Long id, @RequestBody TicketUpdate request) {
        return service.update(auth.getName(), id, request);
    }

    @PostMapping("/management/tickets/{id}/escalate")
    public SupportTicket escalate(Authentication auth, @PathVariable Long id, @RequestBody EscalationRequest request) {
        return service.escalate(auth.getName(), id, request);
    }
}
