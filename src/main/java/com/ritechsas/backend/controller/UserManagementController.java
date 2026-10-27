
package com.ritechsas.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ritechsas.backend.dto.CreateUserRequest;
import com.ritechsas.backend.dto.LoginResponse;
import com.ritechsas.backend.dto.UserResponse;
import com.ritechsas.backend.entity.User;
import com.ritechsas.backend.service.UserManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(
            UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(
            userManagementService.getAllUsers()
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<LoginResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        User user = userManagementService.createUser(request);

        LoginResponse response = new LoginResponse(
            user.getId(),
            user.getFullName(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
