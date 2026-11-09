package com.bethelreport.backend.keycloak;

import static org.mockito.Mockito.verify;

import com.bethelreport.backend.keycloak.client.KeycloakAdminClient;
import com.bethelreport.backend.keycloak.services.KeycloakUserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

  @Mock
  private KeycloakAdminClient adminClient;

  @InjectMocks
  private KeycloakUserService keycloakUserService;

  @Test
  void sendsKeycloakActionEmailWithoutConstructingTheActionLink() {
    keycloakUserService.sendInvitationActionsEmail("keycloak-user-id");

    verify(adminClient).sendExecuteActionsEmail("keycloak-user-id",
        List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"));
  }
}
