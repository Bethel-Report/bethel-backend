package com.bethelreport.backend;

import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.time.LocalDate;
import org.springframework.security.core.context.SecurityContextHolder;

final class AuditTestFixtures {

  private AuditTestFixtures() {
  }

  static void ensureSystemActor(UserRepository users) {
    SecurityContextHolder.clearContext();
    if (users.findByEmailIgnoreCase("admin@bethelreport.local").isPresent()) {
      return;
    }
    User admin = new User();
    admin.setEmail("admin@bethelreport.local");
    admin.setName("Administrador de teste");
    admin.setBirthday(LocalDate.of(2000, 1, 1));
    admin.setActive(false);
    admin.setCity("Fortaleza");
    admin.setAddress("Endereço de teste");
    users.saveAndFlush(admin);
  }
}
