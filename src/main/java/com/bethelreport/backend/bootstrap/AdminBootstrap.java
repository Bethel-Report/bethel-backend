package com.bethelreport.backend.bootstrap;

import com.bethelreport.backend.keycloak.client.KeycloakAdminClient;
import com.bethelreport.backend.keycloak.dtos.KeycloakUserResponse;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.bootstrap.admin.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

  private final KeycloakAdminClient keycloak;
  private final UserRepository users;
  private final PermissionRepository permissions;
  private final String email;

  public AdminBootstrap(KeycloakAdminClient keycloak, UserRepository users,
      PermissionRepository permissions, @Value("${app.bootstrap.admin.email}") String email) {
    this.keycloak = keycloak;
    this.users = users;
    this.permissions = permissions;
    this.email = email;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments arguments) {
    List<KeycloakUserResponse> identities = keycloak.findUsersByEmail(email);
    if (identities == null || identities.size() != 1) {
      throw new IllegalStateException(
          "Execute o bootstrap do Keycloak antes de iniciar o backend.");
    }
    String identityId = identities.getFirst().id();
    User user = users.findByEmailIgnoreCase(email).orElseGet(this::newAdmin);
    if (user.getKeycloakUserId() != null && !user.getKeycloakUserId().equals(identityId)) {
      throw new IllegalStateException(
          "O administrador está vinculado a outra identidade Keycloak.");
    }
    user.setKeycloakUserId(identityId);
    User saved = users.save(user);
    if (!permissions.existsByUserId(saved.getId())) {
      Permission permission = new Permission();
      permission.setUser(saved);
      permission.setRole(Role.GRANDE_CONSELHO);
      permission.setActive(true);
      permissions.save(permission);
    }
  }

  private User newAdmin() {
    User user = new User();
    user.setEmail(email);
    user.setName("Administrador do Grande Conselho");
    user.setBirthday(LocalDate.of(2000, 1, 1));
    user.setCity("Não informado");
    user.setAddress("Não informado");
    user.setActive(true);
    return user;
  }
}
