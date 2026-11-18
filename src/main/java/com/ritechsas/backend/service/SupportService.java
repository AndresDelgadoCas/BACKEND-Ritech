package com.ritechsas.backend.service;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ritechsas.backend.entity.*;
import com.ritechsas.backend.repository.*;
import com.ritechsas.backend.util.SupportTextSanitizer;

@Service
public class SupportService {
    private final SupportTicketRepository tickets;
    private final SupportMessageRepository messages;
    private final UserRepository users;
    private final UserManagementService userManagement;
    private final AuditLogRepository audit;

    public SupportService(SupportTicketRepository tickets, SupportMessageRepository messages,
                          UserRepository users, UserManagementService userManagement, AuditLogRepository audit) {
        this.tickets = tickets;
        this.messages = messages;
        this.users = users;
        this.userManagement = userManagement;
        this.audit = audit;
    }

    private User user(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private SupportTicket ticket(Long id) {
        return tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
    }

    @Transactional
    public SupportTicket create(String username, TicketRequest request) {
        User requester = user(username);
        if (request == null || blank(request.subject()) || blank(request.description())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asunto y descripción son obligatorios");
        }
        if (request.subject().trim().length() > 180) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El asunto no puede superar 180 caracteres");
        if (SupportTextSanitizer.containsPlaintextCredential(request.subject()) || SupportTextSanitizer.containsPlaintextCredential(request.description())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Por seguridad, no incluyas contraseñas ni códigos de acceso en una solicitud de soporte.");
        }
        if (!requester.hasRole("COMPRADOR") && !requester.hasRole("VENDEDOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo compradores y vendedores pueden crear solicitudes de soporte");
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setTicketCode("RIT-SUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        ticket.setRequesterUserId(requester.getId());
        ticket.setRequesterName(requester.getFullName());
        ticket.setRequesterEmail(requester.getEmail());
        ticket.setRequesterRole(requester.hasRole("VENDEDOR") ? "VENDEDOR" : "COMPRADOR");
        ticket.setCategory(valid(request.category(), Set.of("ACCOUNT_ACCESS", "ACCOUNT_RECOVERY", "ORDERS", "LISTINGS", "INVENTORY", "COMPLAINT", "CLAIM", "OTHER"), "OTHER"));
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description().trim());
        return tickets.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> mine(String username) {
        return tickets.findByRequesterUserIdOrderByCreatedAtDesc(user(username).getId());
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> all(String username) {
        User actor = user(username);
        if (actor.hasRole("SOPORTE")) return tickets.findAllByOrderByCreatedAtDesc();
        if (actor.hasRole("ADMINISTRADOR")) return tickets.findByStatusAndEscalatedToOrderByCreatedAtDesc("ESCALATED", actor.getId());
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public List<StaffOption> administrators(String username) {
        User actor = user(username);
        if (!actor.hasRole("SOPORTE")) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return users.findAll().stream()
            .filter(User::isActive)
            .filter(candidate -> candidate.hasRole("ADMINISTRADOR"))
            .map(candidate -> new StaffOption(candidate.getId(), candidate.getFullName(), candidate.getUsername()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> escalatedForAdministrator(String username) {
        User actor = user(username);
        if (!actor.hasRole("ADMINISTRADOR")) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return tickets.findByStatusAndEscalatedToOrderByCreatedAtDesc("ESCALATED", actor.getId());
    }

    @Transactional(readOnly = true)
    public List<SupportMessage> messages(String username, Long ticketId) {
        User actor = user(username);
        SupportTicket ticket = ticket(ticketId);
        boolean supportAgent = actor.hasRole("SOPORTE");
        boolean assignedAdmin = actor.hasRole("ADMINISTRADOR") && Objects.equals(ticket.getEscalatedTo(), actor.getId());
        boolean staff = supportAgent || assignedAdmin;
        if (!staff && !Objects.equals(ticket.getRequesterUserId(), actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return messages.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
            .filter(message -> staff || !message.isInternal())
            .peek(this::attachSenderName)
            .toList();
    }

    @Transactional
    public SupportMessage reply(String username, Long ticketId, MessageRequest request) {
        User actor = user(username);
        SupportTicket ticket = ticket(ticketId);
        boolean supportAgent = actor.hasRole("SOPORTE");
        boolean assignedAdmin = actor.hasRole("ADMINISTRADOR") && Objects.equals(ticket.getEscalatedTo(), actor.getId());
        boolean staff = supportAgent || assignedAdmin;
        if (!staff && !Objects.equals(ticket.getRequesterUserId(), actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (request == null || blank(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje no puede estar vacío");
        }
        if (SupportTextSanitizer.containsPlaintextCredential(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Por seguridad, no compartas contraseñas ni códigos de acceso por el chat de soporte.");
        }
        SupportMessage message = new SupportMessage();
        message.setTicketId(ticketId);
        message.setSenderUserId(actor.getId());
        message.setSenderType(actor.hasRole("ADMINISTRADOR") ? "ADMINISTRATOR" : staff ? "SUPPORT" : "REQUESTER");
        message.setMessage(request.message().trim());
        message.setInternal(staff && Boolean.TRUE.equals(request.internal()));
        SupportMessage saved = messages.save(message);
        attachSenderName(saved);
        return saved;
    }

    @Transactional
    public SupportTicket update(String username, Long id, TicketUpdate request) {
        User actor = user(username);
        SupportTicket ticket = ticket(id);
        boolean support = actor.hasRole("SOPORTE");
        if (!support && !actor.hasRole("ADMINISTRADOR")) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!support && (!"ESCALATED".equals(ticket.getStatus()) || !Objects.equals(ticket.getEscalatedTo(), actor.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes gestionar los casos escalados directamente a tu cuenta");
        }
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay cambios para aplicar");

        if (request.status() != null) {
            String next = request.status().trim().toUpperCase(Locale.ROOT);
            if ("ESCALATED".equals(next)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utiliza la acción Escalar al administrador para enviar el caso");
            }
            if (support && "ESCALATED".equals(ticket.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket está en revisión administrativa; espera la decisión del administrador");
            }
            ticket.setStatus(valid(next, Set.of("NEW", "IN_REVIEW", "WAITING_USER", "RESOLVED", "CLOSED"), ticket.getStatus()));
        }
        if (request.priority() != null) {
            ticket.setPriority(valid(request.priority(), Set.of("LOW", "NORMAL", "HIGH", "URGENT"), ticket.getPriority()));
        }
        if (request.resolution() != null) ticket.setResolution(request.resolution().trim());
        if (request.assignedTo() != null) {
            User assignee = users.findById(request.assignedTo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agente no encontrado"));
            if (!assignee.hasRole("SOPORTE") && !assignee.hasRole("ADMINISTRADOR")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El asignado debe pertenecer a soporte o administración");
            }
            ticket.setAssignedTo(assignee.getId());
        }
        if ("RESOLVED".equals(ticket.getStatus())) ticket.setResolvedAt(LocalDateTime.now());
        if ("CLOSED".equals(ticket.getStatus())) ticket.setClosedAt(LocalDateTime.now());
        return tickets.save(ticket);
    }

    @Transactional
    public SupportTicket escalate(String username, Long id, EscalationRequest request) {
        User actor = user(username);
        if (!actor.hasRole("SOPORTE")) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (request == null || request.adminUserId() == null || blank(request.reason())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un administrador e indica el motivo");
        }
        User admin = users.findById(request.adminUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Administrador no encontrado"));
        if (!admin.isActive() || !admin.hasRole("ADMINISTRADOR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La escalación debe dirigirse a un administrador activo");
        }
        SupportTicket ticket = ticket(id);
        if ("RESOLVED".equals(ticket.getStatus()) || "CLOSED".equals(ticket.getStatus()) || "ESCALATED".equals(ticket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket ya está cerrado o escalado");
        }
        ticket.setStatus("ESCALATED");
        ticket.setEscalatedTo(admin.getId());
        ticket.setEscalationReason(request.reason().trim());
        ticket = tickets.save(ticket);
        addSystemStaffMessage(ticket.getId(), actor, "Caso escalado al administrador " + admin.getFullName() + ". Motivo: " + request.reason().trim(), true);
        writeAudit(actor, "SUPPORT_TICKET_ESCALATED", ticket.getId(), Map.of("adminUserId", admin.getId(), "reason", request.reason().trim()));
        return ticket;
    }

    @Transactional
    public SupportTicket reviewEscalation(String username, Long id, AdminDecisionRequest request) {
        User admin = user(username);
        if (!admin.hasRole("ADMINISTRADOR")) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (request == null || blank(request.decision()) || blank(request.reason())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una decisión y escribe la justificación");
        }
        SupportTicket ticket = ticket(id);
        if (!"ESCALATED".equals(ticket.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket no está pendiente de revisión administrativa");
        if (!Objects.equals(ticket.getEscalatedTo(), admin.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este caso fue escalado a otro administrador");

        String decision = request.decision().trim().toUpperCase(Locale.ROOT);
        String resultMessage;
        switch (decision) {
            case "APPROVE_SUSPENSION" -> {
                if (request.producerId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona el vendedor cuya suspensión estás aprobando");
                userManagement.setSellerStatus(request.producerId(), "SUSPENDED", request.reason(), username);
                resultMessage = "Suspensión comercial aprobada para el productor #" + request.producerId() + ". Motivo: " + request.reason().trim();
            }
            case "REJECT_SUSPENSION" -> resultMessage = "Solicitud de suspensión rechazada. Motivo: " + request.reason().trim();
            case "REACTIVATE_SELLER" -> {
                if (request.producerId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona el vendedor que se va a reactivar");
                userManagement.setSellerStatus(request.producerId(), "ACTIVE", request.reason(), username);
                resultMessage = "Reactivación comercial aprobada para el productor #" + request.producerId() + ". Motivo: " + request.reason().trim();
            }
            case "RESOLVE" -> resultMessage = "Caso revisado por administración. Resolución: " + request.reason().trim();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decisión administrativa no válida");
        }

        ticket.setResolution(resultMessage);
        ticket.setStatus("RESOLVED");
        ticket.setResolvedAt(LocalDateTime.now());
        SupportTicket saved = tickets.save(ticket);
        addSystemStaffMessage(saved.getId(), admin, resultMessage, false);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("decision", decision);
        details.put("reason", request.reason().trim());
        if (request.producerId() != null) details.put("producerId", request.producerId());
        writeAudit(admin, "SUPPORT_TICKET_ADMIN_DECISION", saved.getId(), details);
        return saved;
    }

    private void attachSenderName(SupportMessage message) {
        if (message.getSenderUserId() == null) {
            message.setSenderName(message.getSenderType() != null && message.getSenderType().equals("REQUESTER") ? "Solicitante" : "Equipo de soporte");
            return;
        }
        users.findById(message.getSenderUserId())
            .ifPresent(sender -> message.setSenderName(sender.getFullName()));
    }

    private void writeAudit(User actor, String action, Long ticketId, Map<String, Object> details) {
        AuditLog entry = new AuditLog();
        entry.setActorUserId(actor.getId());
        entry.setAction(action);
        entry.setEntityType("support_tickets");
        entry.setEntityId(ticketId);
        entry.setDetails(details);
        audit.save(entry);
    }

    private void addSystemStaffMessage(Long ticketId, User sender, String text, boolean internal) {
        SupportMessage message = new SupportMessage();
        message.setTicketId(ticketId);
        message.setSenderUserId(sender.getId());
        message.setSenderType(sender.hasRole("ADMINISTRADOR") ? "ADMINISTRATOR" : "SUPPORT");
        message.setMessage(text);
        message.setInternal(internal);
        messages.save(message);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String valid(String value, Set<String> allowed, String fallback) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : fallback;
    }

    public record TicketRequest(String category, String subject, String description) {}
    public record MessageRequest(String message, Boolean internal) {}
    public record TicketUpdate(String status, String priority, String resolution, Long assignedTo) {}
    public record EscalationRequest(Long adminUserId, String reason) {}
    public record AdminDecisionRequest(String decision, Long producerId, String reason) {}
    public record StaffOption(Long id, String fullName, String username) {}
}
