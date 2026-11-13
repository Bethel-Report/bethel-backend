package com.bethelreport.backend.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.invitation")
public record KeycloakInvitationProperties(String clientId, String redirectUri, Integer lifespan) {
}
