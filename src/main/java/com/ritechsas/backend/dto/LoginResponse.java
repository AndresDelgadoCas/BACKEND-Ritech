package com.ritechsas.backend.dto;

import java.util.Set;

public record LoginResponse(
    Long id,
    String fullName,
    String username,
    String email,
    String role,
    Set<String> roles
) {
}
