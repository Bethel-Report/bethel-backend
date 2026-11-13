package com.bethelreport.backend.keycloak.services;

import com.bethelreport.backend.keycloak.client.KeycloakAdminClient;
import com.bethelreport.backend.keycloak.dtos.KeycloakUserRequest;
import com.bethelreport.backend.keycloak.dtos.KeycloakUserResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KeycloakUserService {

  private final KeycloakAdminClient adminClient;

  public KeycloakUserService(KeycloakAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public void deleteUser(String userId) {
    adminClient.deleteUser(userId);
  }

  public String provisionUser(String email, String firstName, String lastName) {
    List<KeycloakUserResponse> existingUsers = adminClient.findUsersByEmail(email);

    if (existingUsers != null && !existingUsers.isEmpty()) {
      throw new IllegalStateException("Já existe uma identidade no Keycloak para este e-mail.");
    }

    KeycloakUserRequest request = new KeycloakUserRequest(email, email, firstName, lastName, true,
        false, List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"));

    return adminClient.createUser(request);
  }

  public void sendInvitationActionsEmail(String userId) {
    adminClient.sendExecuteActionsEmail(userId, List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"));
  }
}
