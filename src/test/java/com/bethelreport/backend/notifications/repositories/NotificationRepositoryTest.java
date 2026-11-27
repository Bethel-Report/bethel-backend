package com.bethelreport.backend.notifications.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationAudience;
import com.bethelreport.backend.publications.entities.PublicationType;
import com.bethelreport.backend.publications.repositories.PublicationRepository;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class NotificationRepositoryTest {
  private static final String SYSTEM_ADMIN_EMAIL = "admin@bethelreport.local";

  @Container
  @ServiceConnection
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
      DockerImageName.parse("postgres:latest"));

  @Container
  @ServiceConnection
  private static final RabbitMQContainer RABBIT = new RabbitMQContainer(
      DockerImageName.parse("rabbitmq:latest"));

  @Autowired
  private NotificationRepository notificationRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private BethelRepository bethelRepository;
  @Autowired
  private PermissionRepository permissionRepository;
  @Autowired
  private PublicationRepository publicationRepository;

  @Test
  @Transactional
  void publicationRecipientsRespectExplicitUserAndBethelAudiences() {
    String suffix = UUID.randomUUID().toString();
    User globalAdmin = userRepository.findByEmailIgnoreCase(SYSTEM_ADMIN_EMAIL)
        .orElseGet(() -> userRepository.saveAndFlush(systemAdmin(suffix)));
    Bethel firstBethel = bethelRepository.save(bethel("First", suffix, 201));
    Bethel secondBethel = bethelRepository.save(bethel("Second", suffix, 202));
    User firstUser = userRepository.save(user("first", suffix));
    User secondUser = userRepository.save(user("second", suffix));
    permission(firstUser, firstBethel, Role.MEMBRO_ATIVO);
    permission(secondUser, secondBethel, Role.MEMBRO_ATIVO);
    permission(globalAdmin, null, Role.GRANDE_CONSELHO);

    Publication userTargeted = publication(firstUser, PublicationAudience.USERS, suffix);
    userTargeted.setTargetUserIds(Set.of(firstUser.getId()));
    userTargeted = publicationRepository.saveAndFlush(userTargeted);

    Publication bethelTargeted = publication(globalAdmin, PublicationAudience.BETHELS, suffix);
    bethelTargeted.setTargetBethelIds(Set.of(secondBethel.getId()));
    bethelTargeted = publicationRepository.saveAndFlush(bethelTargeted);

    assertEquals(List.of(firstUser.getId()),
        notificationRepository.findPublicationRecipientIds(userTargeted.getId()));
    assertEquals(List.of(secondUser.getId()),
        notificationRepository.findPublicationRecipientIds(bethelTargeted.getId()));
  }

  private void permission(User user, Bethel bethel, Role role) {
    Permission permission = new Permission();
    permission.setUser(user);
    permission.setBethel(bethel);
    permission.setRole(role);
    permissionRepository.save(permission);
  }

  private User user(String name, String suffix) {
    User user = new User();
    user.setEmail(name + "." + suffix + "@example.test");
    user.setName(name);
    user.setBirthday(LocalDate.of(2000, 1, 1));
    user.setActive(true);
    user.setCity("Fortaleza");
    user.setAddress("Rua de teste");
    return user;
  }

  private User systemAdmin(String suffix) {
    User user = user("system-admin", suffix);
    user.setEmail(SYSTEM_ADMIN_EMAIL);
    return user;
  }

  private Bethel bethel(String name, String suffix, int number) {
    Bethel bethel = new Bethel();
    bethel.setName(name + " " + suffix);
    bethel.setNumber(number);
    bethel.setCity("Fortaleza");
    bethel.setAddress("Rua de teste");
    bethel.setEmail(name.toLowerCase() + "." + suffix + "@example.test");
    bethel.setSponsoringLodge("Loja de teste");
    bethel.setFoundationDate(LocalDate.of(2000, 1, 1));
    bethel.setActive(true);
    return bethel;
  }

  private Publication publication(User author, PublicationAudience audience, String suffix) {
    Publication publication = new Publication();
    publication.setTitle("Aviso " + suffix);
    publication.setContent("Conteúdo da publicação");
    publication.setType(PublicationType.GUIDANCE);
    publication.setAudience(audience);
    publication.setAuthor(author);
    publication.setTimelineAt(LocalDateTime.now());
    publication.setCreatedAt(LocalDateTime.now());
    publication.setUpdatedAt(LocalDateTime.now());
    publication.setDisplayFrom(LocalDateTime.now().minusMinutes(1));
    publication.setDisplayUntil(LocalDateTime.now().plusDays(1));
    return publication;
  }
}
