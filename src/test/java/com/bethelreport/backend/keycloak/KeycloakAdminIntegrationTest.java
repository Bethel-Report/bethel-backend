package com.bethelreport.backend.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import com.bethelreport.backend.keycloak.client.KeycloakAdminClient;
import com.bethelreport.backend.keycloak.config.KeycloakAdminProperties;
import com.bethelreport.backend.keycloak.services.KeycloakAdminTokenService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "RUN_KEYCLOAK_INTEGRATION_TESTS", matches = "true")
class KeycloakAdminIntegrationTest {

  private static KeycloakAdminTokenService tokenService;

  private static KeycloakAdminClient adminClient;

  @BeforeAll
  static void setUp() {
    KeycloakAdminProperties properties = new KeycloakAdminProperties(
        requiredEnvironmentVariable("KEYCLOAK_ADMIN_SERVER_URL"),
        requiredEnvironmentVariable("KEYCLOAK_ADMIN_REALM"),
        requiredEnvironmentVariable("KEYCLOAK_ADMIN_CLIENT_ID"),
        requiredEnvironmentVariable("KEYCLOAK_ADMIN_CLIENT_SECRET"));
    tokenService = new KeycloakAdminTokenService(properties);
    adminClient = new KeycloakAdminClient(properties, tokenService);
  }

  @Test
  void shouldObtainAdminAccessToken() {
    String token = tokenService.getAccessToken();

    assertThat(token).isNotBlank();
  }

  @Test
  void shouldAccessAdminApi() {
    var users = adminClient.findUsers();

    assertThat(users).isNotNull();
  }

  private static String requiredEnvironmentVariable(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Defina a variável de ambiente " + name + " para executar "
          + "os testes de integração do Keycloak.");
    }
    return value;
  }
}
