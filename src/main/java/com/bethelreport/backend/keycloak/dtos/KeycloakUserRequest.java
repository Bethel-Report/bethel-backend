package com.bethelreport.backend.keycloak.dtos;

import java.util.List;

public record KeycloakUserRequest(String username, String email, String firstName, String lastName,
    Boolean enabled, Boolean emailVerified, List<String> requiredActions) {
}
