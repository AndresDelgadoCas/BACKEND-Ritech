package com.ritechsas.backend.controller;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import com.ritechsas.backend.dto.MarketplaceDtos.*;
import com.ritechsas.backend.dto.ClientRequest;
import jakarta.validation.Valid;
import com.ritechsas.backend.entity.AuditLog;
import com.ritechsas.backend.service.UserManagementService;
import com.ritechsas.backend.repository.AuditLogRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserManagementService users;
    private final AuditLogRepository audit;
    public AdminController(UserManagementService users, AuditLogRepository audit) { this.users = users; this.audit = audit; }

    @GetMapping("/profiles") public List<ProfileOption> availableProfiles(@RequestParam String role) { return users.availableProfiles(role); }
    @PatchMapping("/users/{id}/profile") public void assignProfile(Authentication auth, @PathVariable Long id, @RequestBody ProfileAssignmentRequest request) {
        if (request == null || request.profileId() == null || request.profileType() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un perfil válido");
        users.assignProfile(id, request.profileType(), request.profileId(), auth.getName());
    }
    @PostMapping("/users/{id}/client-profile")
    public void createClientProfile(Authentication auth, @PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        users.createClientProfile(id, request, auth.getName());
    }
    @PatchMapping("/users/{id}/active") public void setActive(Authentication auth, @PathVariable Long id, @RequestBody UserStatusRequest request) {
        if (request == null || request.active() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        users.updateActive(id, request.active(), auth.getName());
    }
    @PatchMapping("/users/{id}/role") public void setRole(Authentication auth, @PathVariable Long id, @RequestBody UserRoleRequest request) {
        if (request == null || request.role() == null || request.role().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un rol válido");
        users.updateRole(id, request.role(), auth.getName());
    }
    @PatchMapping("/users/{id}/password") public void resetPassword(Authentication auth, @PathVariable Long id, @RequestBody PasswordResetRequest request) {
        if (request == null || request.password() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        users.resetPassword(id, request.password(), auth.getName());
    }
    @GetMapping("/audit") public List<AuditLog> audit() { return audit.findAll(Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))); }
}
