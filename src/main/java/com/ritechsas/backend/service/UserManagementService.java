
package com.ritechsas.backend.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ritechsas.backend.dto.CreateUserRequest;
import com.ritechsas.backend.dto.UserResponse;
import com.ritechsas.backend.entity.Role;
import com.ritechsas.backend.entity.User;
import com.ritechsas.backend.repository.RoleRepository;
import com.ritechsas.backend.repository.UserRepository;
import com.ritechsas.backend.repository.AuditLogRepository;
import com.ritechsas.backend.repository.ProducerRepository;
import com.ritechsas.backend.repository.ClientRepository;
import com.ritechsas.backend.entity.Producer;
import com.ritechsas.backend.entity.Client;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditRepository;
    private final ProducerRepository producerRepository;
    private final ClientRepository clientRepository;

    public UserManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditLogRepository auditRepository,
            ProducerRepository producerRepository,
            ClientRepository clientRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditRepository = auditRepository;
        this.producerRepository = producerRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public User createUser(CreateUserRequest request, String actorUsername) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String roleName = request.role().trim().toUpperCase(Locale.ROOT);

        if (!List.of("ADMINISTRADOR", "COMPRADOR").contains(roleName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Solo se pueden crear cuentas ADMINISTRADOR o COMPRADOR. El rol VENDEDOR requiere aprobación.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya está registrado");
        }

        if ("COMPRADOR".equals(roleName)) {
            if (request.companyName() == null || request.companyName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la empresa del comprador es obligatorio");
            }
            if (request.country() == null || request.country().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El país del comprador es obligatorio");
            }
        }

        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol indicado no existe"));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.getRoles().add(role);
        user.setActive(true);
        User saved = userRepository.save(user);

        boolean clientCreated = false;
        if ("COMPRADOR".equals(roleName)) {
            Client client = new Client();
            client.setCompanyName(request.companyName().trim());
            String contact = request.contactName() == null || request.contactName().isBlank()
                ? saved.getFullName() : request.contactName().trim();
            client.setContactName(contact);
            client.setCountry(request.country().trim());
            client.setEmail(email);
            client.setPhone(blankToNull(request.phone()));
            client.setAddress(blankToNull(request.address()));
            client.setUserId(saved.getId());
            clientRepository.save(client);
            clientCreated = true;
        }

        User actor = userRepository.findByUsername(actorUsername).orElseThrow();
        writeAudit(actor, "USER_CREATED", saved.getId(),
            Map.of("role", roleName, "username", username, "clientProfileCreated", clientCreated));
        return saved;
    }

    @Transactional
    public void createClientProfile(Long userId, com.ritechsas.backend.dto.ClientRequest request, String actorUsername) {
        User actor = userRepository.findByUsername(actorUsername).orElseThrow();
        User target = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!target.hasRole("COMPRADOR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo una cuenta COMPRADOR puede tener perfil de cliente");
        }
        if (clientRepository.findByUserId(userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta cuenta ya tiene un perfil de comprador");
        }
        Client client = new Client();
        client.setCompanyName(request.companyName().trim());
        client.setContactName(request.contactName().trim());
        client.setCountry(request.country().trim());
        client.setEmail(request.email() == null || request.email().isBlank() ? target.getEmail() : request.email().trim().toLowerCase(Locale.ROOT));
        client.setPhone(blankToNull(request.phone()));
        client.setAddress(blankToNull(request.address()));
        client.setUserId(userId);
        Client saved = clientRepository.save(client);
        writeAudit(actor, "BUYER_PROFILE_CREATED", userId, Map.of("clientId", saved.getId(), "companyName", saved.getCompanyName()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional
    public void updateActive(Long userId, boolean active, String actorUsername) {
        User actor = userRepository.findByUsername(actorUsername).orElseThrow();
        User target = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (actor.getId().equals(target.getId()) && !active) throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes desactivar tu propia cuenta");
        target.setActive(active); userRepository.save(target);
        writeAudit(actor, "USER_STATUS_CHANGED", userId, Map.of("active", active));
    }

    @Transactional
    public void updateRole(Long userId, String roleValue, String actorUsername) {
        String roleName = roleValue == null ? "" : roleValue.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ADMINISTRADOR", "COMPRADOR").contains(roleName)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol VENDEDOR solo se concede al aprobar una solicitud");
        User actor = userRepository.findByUsername(actorUsername).orElseThrow();
        User target = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol no existe en la base de datos"));
        target.getRoles().add(role);
        target.setRole(role); // Legacy column kept for compatibility with existing data and code.
        userRepository.save(target);
        writeAudit(actor, "USER_ROLE_ADDED", userId, Map.of("role", roleName));
    }

    @Transactional
    public void resetPassword(Long userId, String password, String actorUsername) {
        if (password == null || password.length() < 12 || password.length() > 72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener entre 12 y 72 caracteres");
        User actor = userRepository.findByUsername(actorUsername).orElseThrow();
        User target = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        target.setPasswordHash(passwordEncoder.encode(password)); userRepository.save(target);
        writeAudit(actor, "USER_PASSWORD_RESET", userId, Map.of("passwordReset", true));
    }

    @Transactional(readOnly = true)
    public List<com.ritechsas.backend.dto.MarketplaceDtos.ProfileOption> availableProfiles(String role) {
        if ("VENDEDOR".equalsIgnoreCase(role)) return producerRepository.findByUserIdIsNull().stream().map(p -> new com.ritechsas.backend.dto.MarketplaceDtos.ProfileOption(p.getId(), p.getName(), p.getContact(), null)).toList();
        if ("COMPRADOR".equalsIgnoreCase(role)) return clientRepository.findByUserIdIsNull().stream().map(c -> new com.ritechsas.backend.dto.MarketplaceDtos.ProfileOption(c.getId(), c.getCompanyName(), c.getContactName() + " · " + c.getCountry(), null)).toList();
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El administrador no requiere perfil comercial");
    }

    @Transactional
    public void assignProfile(Long userId, String profileType, Long profileId, String actorUsername) {
        User actor = userRepository.findByUsername(actorUsername).orElseThrow();
        User target = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        String type = profileType == null ? "" : profileType.trim().toUpperCase(Locale.ROOT);
        if (target.hasRole("VENDEDOR") && "PRODUCER".equals(type)) {
            producerRepository.findByUserId(target.getId()).ifPresent(old -> { old.setUserId(null); producerRepository.save(old); });
            Producer p = producerRepository.findById(profileId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de productor no encontrado"));
            if (p.getUserId() != null && !p.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese perfil ya está vinculado a otra cuenta");
            p.setUserId(userId); producerRepository.save(p);
        } else if (target.hasRole("COMPRADOR") && "CLIENT".equals(type)) {
            clientRepository.findByUserId(target.getId()).ifPresent(old -> { old.setUserId(null); clientRepository.save(old); });
            Client c = clientRepository.findById(profileId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de cliente no encontrado"));
            if (c.getUserId() != null && !c.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese perfil ya está vinculado a otra cuenta");
            c.setUserId(userId); clientRepository.save(c);
        } else { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de perfil no corresponde al rol de la cuenta"); }
        writeAudit(actor, "USER_PROFILE_LINKED", userId, Map.of("profileType", type, "profileId", profileId));
    }

    private void writeAudit(User actor, String action, Long entityId, Map<String,Object> details) {
        com.ritechsas.backend.entity.AuditLog log = new com.ritechsas.backend.entity.AuditLog();
        log.setActorUserId(actor.getId()); log.setAction(action); log.setEntityType("users"); log.setEntityId(entityId); log.setDetails(details); auditRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(user -> {
            Client client = user.hasRole("COMPRADOR")
                ? clientRepository.findByUserId(user.getId()).orElse(null) : null;
            Producer producer = user.hasRole("VENDEDOR")
                ? producerRepository.findByUserId(user.getId()).orElse(null) : null;
            String clientLabel = client == null ? null : client.getCompanyName();
            String producerLabel = producer == null ? null : producer.getName();
            String combinedLabel = java.util.stream.Stream.of(
                    clientLabel == null ? null : "Cliente: " + clientLabel,
                    producerLabel == null ? null : "Productor: " + producerLabel)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" · "));
            if (combinedLabel.isBlank()) combinedLabel = null;
            Long profileId = producer != null ? producer.getId() : client != null ? client.getId() : null;
            return new UserResponse(
                user.getId(), user.getFullName(), user.getUsername(), user.getEmail(),
                user.getRole().getName(),
                user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()),
                user.isActive(), profileId, combinedLabel, clientLabel, producerLabel);
        }).toList();
    }
}
