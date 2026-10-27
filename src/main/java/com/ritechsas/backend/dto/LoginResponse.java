
package com.ritechsas.backend.dto;

public record LoginResponse(
    Long id,
    String fullName,
    String username,
    String email,
    String role
) {
}
