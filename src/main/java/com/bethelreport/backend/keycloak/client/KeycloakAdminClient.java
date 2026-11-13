package com.bethelreport.backend.keycloak.client;

import com.bethelreport.backend.keycloak.config.KeycloakAdminProperties;
import com.bethelreport.backend.keycloak.config.KeycloakInvitationProperties;
import com.bethelreport.backend.keycloak.dtos.KeycloakUserRequest;
import com.bethelreport.backend.keycloak.dtos.KeycloakUserResponse;
import com.bethelreport.backend.keycloak.services.KeycloakAdminTokenService;
import java.net.URI;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KeycloakAdminClient {

  private final RestClient restClient;
  private final KeycloakAdminTokenService tokenService;
  private final KeycloakInvitationProperties invitationProperties;

  public KeycloakAdminClient(KeycloakAdminProperties properties,
      KeycloakAdminTokenService tokenService, KeycloakInvitationProperties invitationProperties) {
    this.restClient = RestClient.builder()
        .baseUrl(properties.serverUrl() + "/admin/realms/" + properties.realm()).build();

    this.tokenService = tokenService;
    this.invitationProperties = invitationProperties;
  }

  public List<KeycloakUserResponse> findUsers() {
    return restClient.get().uri("/users")
        .headers(headers -> headers.setBearerAuth(tokenService.getAccessToken())).retrieve()
        .body(new ParameterizedTypeReference<>() {
        });
  }

  public List<KeycloakUserResponse> findUsersByEmail(String email) {
    return restClient.get()
        .uri(uriBuilder -> uriBuilder.path("/users").queryParam("email", email)
            .queryParam("exact", true).build())
        .headers(headers -> headers.setBearerAuth(tokenService.getAccessToken())).retrieve()
        .body(new ParameterizedTypeReference<>() {
        });
  }

  public String createUser(KeycloakUserRequest request) {
    ResponseEntity<Void> response = restClient.post().uri("/users")
        .headers(headers -> headers.setBearerAuth(tokenService.getAccessToken()))
        .contentType(MediaType.APPLICATION_JSON).body(request).retrieve().toBodilessEntity();

    URI location = response.getHeaders().getLocation();

    if (location == null) {
      throw new IllegalStateException(
          "Keycloak criou o usuário, mas não retornou sua localização.");
    }

    String path = location.getPath();

    return path.substring(path.lastIndexOf('/') + 1);

  }

  public void sendExecuteActionsEmail(String userId, List<String> actions) {
    restClient.put()
        .uri(uriBuilder -> uriBuilder.path("/users/{id}/execute-actions-email")
            .queryParam("client_id", invitationProperties.clientId())
            .queryParam("redirect_uri", invitationProperties.redirectUri())
            .queryParam("lifespan", invitationProperties.lifespan()).build(userId))
        .headers(headers -> headers.setBearerAuth(tokenService.getAccessToken()))
        .contentType(MediaType.APPLICATION_JSON).body(actions).retrieve().toBodilessEntity();
  }

  public void deleteUser(String userId) {
    restClient.method(HttpMethod.DELETE).uri("/users/{id}", userId)
        .headers(headers -> headers.setBearerAuth(tokenService.getAccessToken())).retrieve()
        .toBodilessEntity();
  }
}
