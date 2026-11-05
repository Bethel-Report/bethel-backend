package com.bethelreport.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationAttachment;
import com.bethelreport.backend.publications.entities.PublicationAudience;
import com.bethelreport.backend.publications.entities.PublicationType;
import com.bethelreport.backend.publications.repositories.PublicationAttachmentRepository;
import com.bethelreport.backend.publications.repositories.PublicationRepository;
import com.bethelreport.backend.publications.services.PublicationAccess;
import com.bethelreport.backend.publications.services.PublicationDeletionService;
import com.bethelreport.backend.storage.MinioStorage;
import com.bethelreport.backend.storage.StorageDeletionQueue;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Import(PublicationRepositoryTest.Database.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PublicationRepositoryTest {
  @TestConfiguration(proxyBeanMethods = false)
  static class Database {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
      return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }
  }

  @Autowired
  private PublicationRepository publications;
  @Autowired
  private PublicationAttachmentRepository attachments;
  @Autowired
  private PermissionRepository permissions;
  @Autowired
  private DataSource dataSource;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private BethelRepository bethelRepository;

  @Test
  @Transactional
  void feedFiltersAudienceAndPeriodInDatabaseAndPaginatesInBothDirections() {
    String suffix = UUID.randomUUID().toString();
    User author = userRepository.save(createUser("Author", suffix, true));
    User reader = userRepository.save(createUser("Reader", suffix, true));
    Bethel own = bethelRepository.save(createBethel(901, suffix));
    Bethel other = bethelRepository.save(createBethel(902, suffix));
    Publication statewide = save(author, null, PublicationAudience.ALL_BETHELS, Set.of(), Set.of());
    Publication local = save(author, own, PublicationAudience.BETHELS, Set.of(own.getId()),
        Set.of());
    Publication foreign = save(author, other, PublicationAudience.BETHELS, Set.of(other.getId()),
        Set.of());
    Publication targeted = save(author, null, PublicationAudience.USERS, Set.of(),
        Set.of(reader.getId()));
    Publication hidden = save(author, own, PublicationAudience.BETHELS, Set.of(own.getId()),
        Set.of());
    hidden.setDisplayUntil(LocalDateTime.now().minusDays(1));
    hidden.setDisplayFrom(LocalDateTime.now().minusDays(2));
    publications.saveAndFlush(hidden);
    var descending = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "timelineAt", "id"));
    LocalDateTime now = LocalDateTime.now();
    var visible = publications.findFeed(false, false, own.getId(), reader.getId(), now, null, null,
        descending);
    assertEquals(3, visible.getTotalElements());
    assertEquals(List.of(targeted.getId()), visible.map(Publication::getId).getContent());
    assertEquals(List.of(local.getId()),
        publications
            .findFeed(false, false, own.getId(), reader.getId(), now, null, null, descending.next())
            .map(Publication::getId).getContent());
    var ascending = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "timelineAt", "id"));
    assertEquals(List.of(statewide.getId(), local.getId(), targeted.getId()),
        publications.findFeed(false, false, own.getId(), reader.getId(), now, null, null, ascending)
            .map(Publication::getId).getContent());
    assertFalse(
        publications.findFeed(false, false, own.getId(), reader.getId(), now, null, null, ascending)
            .map(Publication::getId).getContent().contains(foreign.getId()));
    assertEquals(2,
        publications.findFeed(true, false, own.getId(), reader.getId(), now, null, null, ascending)
            .getTotalElements());
    assertEquals(5, publications
        .findFeed(true, true, null, author.getId(), now, null, null, ascending).getTotalElements());
    assertEquals(3,
        publications
            .findFeed(false, false, own.getId(), reader.getId(), now, own.getId(), null, ascending)
            .getTotalElements());
    assertEquals(0, publications.findFeed(false, false, own.getId(), reader.getId(), now, null,
        PublicationType.ACTIVITY, ascending).getTotalElements());
    assertEquals(1,
        publications.findFeed(false, false, null, author.getId(), now, null, null, ascending)
            .getTotalElements());
    targeted.setBethel(own);
    publications.saveAndFlush(targeted);
    assertEquals(2,
        publications
            .findFeed(false, false, other.getId(), reader.getId(), now, null, null, ascending)
            .getTotalElements());
  }

  @Test
  void deletingPublicationPersistsCleanupAndRemovesAttachmentsAndAudienceLinks() {
    String suffix = UUID.randomUUID().toString();
    User author = userRepository.save(createUser("Author", suffix, true));
    Bethel owner = bethelRepository.save(createBethel(903, suffix));
    Permission permission = new Permission();
    permission.setUser(author);
    permission.setBethel(owner);
    permission.setRole(Role.CONSELHO_DE_BETHEL);
    permissions.saveAndFlush(permission);
    Publication publication = save(author, owner, PublicationAudience.BETHELS,
        Set.of(owner.getId()), Set.of());
    PublicationAttachment attachment = new PublicationAttachment();
    attachment.setPublication(publication);
    attachment.setObjectKey("documentos/publicacoes/" + suffix);
    attachment.setFilename("test.pdf");
    attachment.setContentType("application/pdf");
    attachment.setSize(10);
    attachment.setCreatedAt(LocalDateTime.now());
    attachments.saveAndFlush(attachment);
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    MinioStorage storage = mock(MinioStorage.class);
    PublicationDeletionService deletion = new PublicationDeletionService(publications, attachments,
        new PublicationAccess(permissions), new StorageDeletionQueue(jdbc));
    Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").claim("email", author.getEmail())
        .build();
    deletion.delete(publication.getId(), new JwtAuthenticationToken(jwt));
    publications.flush();
    assertFalse(publications.existsById(publication.getId()));
    assertFalse(attachments.existsById(attachment.getId()));
    assertEquals(0,
        jdbc.queryForObject(
            "SELECT count(*) FROM publication_target_bethels WHERE publication_id = ?",
            Integer.class, publication.getId()));
    assertEquals(1,
        jdbc.queryForObject("SELECT count(*) FROM storage_deletions WHERE object_key = ?",
            Integer.class, attachment.getObjectKey()));
    verify(storage, never()).delete(any());
  }

  private Publication save(User author, Bethel bethel, PublicationAudience audience,
      Set<Long> targetBethels, Set<Long> targetUsers) {
    Publication publication = new Publication();
    publication.setTitle("News");
    publication.setContent("Content");
    publication.setType(PublicationType.GUIDANCE);
    publication.setAuthor(author);
    publication.setBethel(bethel);
    publication.setAudience(audience);
    publication.setTargetBethelIds(new HashSet<>(targetBethels));
    publication.setTargetUserIds(new HashSet<>(targetUsers));
    publication.setCreatedAt(LocalDateTime.of(2026, 11, 1, 10, 0));
    publication.setUpdatedAt(publication.getCreatedAt());
    publication.setTimelineAt(publication.getCreatedAt());
    return publications.saveAndFlush(publication);
  }

  private User createUser(String name, String suffix, boolean active) {
    User user = new User();
    user.setEmail(name.toLowerCase().replace(' ', '.') + "." + suffix + "@example.com");
    user.setName(name);
    user.setBirthday(LocalDate.of(2000, 1, 1));
    user.setActive(active);
    user.setCity("Fortaleza");
    user.setAddress("Rua de teste");
    return user;
  }

  private Bethel createBethel(int number, String suffix) {
    Bethel bethel = new Bethel();
    bethel.setNumber(number);
    bethel.setName("Bethel " + number + " " + suffix);
    bethel.setCity("Fortaleza");
    bethel.setAddress("Rua de teste");
    bethel.setEmail("bethel" + number + "." + suffix + "@example.com");
    bethel.setSponsoringLodge("Loja de teste");
    bethel.setFoundationDate(LocalDate.of(2000, 1, 1));
    return bethel;
  }
}
