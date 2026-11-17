package com.ritechsas.backend.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import com.ritechsas.backend.entity.*;
import com.ritechsas.backend.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {
    public record RegistrationRequest(@NotBlank @Size(max=120) String fullName,
        @NotBlank @Size(min=3,max=60) String username, @NotBlank @Email @Size(max=150) String email,
        @NotBlank @Size(min=12,max=72) String password, @NotBlank @Size(max=180) String companyName,
        @NotBlank @Size(max=100) String country, @Size(max=30) String phone, @Size(max=255) String address) {}
    private final UserRepository users; private final RoleRepository roles; private final ClientRepository clients; private final PasswordEncoder encoder;
    public RegistrationController(UserRepository users, RoleRepository roles, ClientRepository clients, PasswordEncoder encoder){this.users=users;this.roles=roles;this.clients=clients;this.encoder=encoder;}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public Map<String,Object> register(@Valid @RequestBody RegistrationRequest request){
        String username=request.username().trim(); String email=request.email().trim().toLowerCase();
        if(users.existsByUsername(username)) throw new ResponseStatusException(HttpStatus.CONFLICT,"El nombre de usuario ya está registrado");
        if(users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"El correo ya está registrado");
        Role buyer=roles.findByName("COMPRADOR").orElseThrow(()->new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"No existe el rol COMPRADOR"));
        User user=new User(); user.setFullName(request.fullName().trim()); user.setUsername(username); user.setEmail(email); user.setPasswordHash(encoder.encode(request.password())); user.setRole(buyer); user.getRoles().add(buyer); user.setActive(true); user=users.save(user);
        Client client=new Client(); client.setCompanyName(request.companyName().trim()); client.setContactName(request.fullName().trim()); client.setCountry(request.country().trim()); client.setEmail(email); client.setPhone(request.phone()); client.setAddress(request.address()); client.setUserId(user.getId()); clients.save(client);
        return Map.of("message","Cuenta de comprador creada correctamente. Ya puedes iniciar sesión.");
    }
}
