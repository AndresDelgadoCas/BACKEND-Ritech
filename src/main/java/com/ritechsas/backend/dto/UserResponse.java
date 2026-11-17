package com.ritechsas.backend.dto;

import java.util.Set;

public record UserResponse(
    Long id,
    String fullName,
    String username,
    String email,
    String role,
    Set<String> roles,
    boolean active,
    Long profileId,
    String profileLabel,
    String clientProfileLabel,
    String producerProfileLabel
) {}
