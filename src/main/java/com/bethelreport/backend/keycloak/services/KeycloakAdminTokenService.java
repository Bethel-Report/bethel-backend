package com.bethelreport.backend.keycloak.services;

import com.bethelreport.backend.keycloak.config.KeycloakAdminProperties;
import com.bethelreport.backend.keycloak.dtos.KeycloakTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class KeycloakAdminTokenService {

  private final RestClient restClient;
  private final KeycloakAdminProperties properties;

  public KeycloakAdminTokenService(KeycloakAdminProperties properties) {
    this.restClient = RestClient.create();
    this.properties = properties;
  }

  public String getAccessToken() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");

    String tokenUrl = properties.serverUrl() + "/realms/" + properties.realm()
        + "/protocol/openid-connect/token";

    KeycloakTokenResponse response = restClient.post().uri(tokenUrl)
        .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve()
        .body(KeycloakTokenResponse.class);

    if (response == null || response.accessToken() == null) {
      throw new IllegalStateException("Keycloak não retornou um access token.");
    }

    return response.accessToken();
  }
}
