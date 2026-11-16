package com.bethelreport.backend.keycloak.dtos;

public record KeycloakUserResponse(String id, String username, String email, Boolean enabled,
    Boolean emailVerified) {
}
