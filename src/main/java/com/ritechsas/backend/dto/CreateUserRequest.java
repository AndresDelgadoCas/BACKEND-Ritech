package com.ritechsas.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120)
    String fullName,

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 3, max = 60)
    String username,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150)
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, max = 72,
          message = "La contraseña debe tener entre 12 y 72 caracteres")
    String password,

    @NotBlank(message = "El rol es obligatorio")
    String role
) {}