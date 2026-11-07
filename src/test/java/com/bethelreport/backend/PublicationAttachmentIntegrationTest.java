package com.bethelreport.backend;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationAudience;
import com.bethelreport.backend.publications.entities.PublicationType;
import com.bethelreport.backend.publications.mappers.PublicationMapperImpl;
import com.bethelreport.backend.publications.repositories.PublicationAttachmentRepository;
import com.bethelreport.backend.publications.repositories.PublicationRepository;
import com.bethelreport.backend.publications.services.PublicationAccess;
import com.bethelreport.backend.publications.services.PublicationAttachmentService;
import com.bethelreport.backend.storage.MinioStorage;
import com.bethelreport.backend.storage.MinioTestContainer;
import com.bethelreport.backend.storage.StorageCleanup;
import com.bethelreport.backend.storage.UploadPolicy;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PublicationAttachmentIntegrationTest.Database.class, PublicationAttachmentService.class,
    PublicationAccess.class, PublicationMapperImpl.class, StorageCleanup.class, MinioStorage.class,
    UploadPolicy.class})
@TestPropertySource(properties = {"storage.minio.access-key=integration-test",
    "storage.minio.secret-key=integration-test-secret", "storage.minio.bucket=attachments-test",
    "storage.upload.allowed-extensions=pdf", "storage.upload.max-size=10MB"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
class PublicationAttachmentIntegrationTest {
  private static final AtomicInteger NUMBERS = new AtomicInteger(1500);
  private static final byte[] PDF = "%PDF-1.7 integration".getBytes(StandardCharsets.US_ASCII);
  @Container
  private static final MinioTestContainer MINIO = new MinioTestContainer();

  @TestConfiguration(proxyBeanMethods = false)
  static class Database {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
      return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }
  }

  @DynamicPropertySource
  static void storageProperties(DynamicPropertyRegistry registry) {
    registry.add("storage.minio.endpoint", MINIO::endpoint);
  }

  @Autowired
  private PublicationAttachmentService service;
  @Autowired
  private PublicationAttachmentRepository attachments;
  @Autowired
  private PublicationRepository publications;
  @Autowired
  private UserRepository users;
  @Autowired
  private BethelRepository bethels;
  @Autowired
  private PermissionRepository permissions;
  @Autowired
  private MinioStorage storage;
  @Autowired
  private StorageCleanup cleanup;
  @Autowired
  private JdbcTemplate jdbc;
  @Autowired
  private PlatformTransactionManager transactions;
  private Long publicationId;
  private JwtAuthenticationToken authentication;

  @BeforeEach
  void createPublication() {
    new TransactionTemplate(transactions).executeWithoutResult(status -> {
      String suffix = UUID.randomUUID().toString();
      User user = new User();
      user.setEmail(suffix + "@example.com");
      user.setName("Author");
      user.setBirthday(LocalDate.of(2000, 1, 1));
      user.setActive(true);
      user.setCity("Fortaleza");
      user.setAddress("Rua de teste");
      users.save(user);
      Bethel bethel = new Bethel();
      bethel.setNumber(NUMBERS.incrementAndGet());
      bethel.setName("Bethel " + suffix);
      bethel.setCity("Fortaleza");
      bethel.setAddress("Rua de teste");
      bethel.setEmail("bethel-" + suffix + "@example.com");
      bethel.setSponsoringLodge("Loja de teste");
      bethels.save(bethel);
      Permission permission = new Permission();
      permission.setUser(user);
      permission.setBethel(bethel);
      permission.setRole(Role.CONSELHO_DE_BETHEL);
      permissions.save(permission);
      Publication publication = new Publication();
      publication.setTitle("Orientação");
      publication.setContent("Documento complementar");
      publication.setType(PublicationType.GUIDANCE);
      publication.setAudience(PublicationAudience.BETHELS);
      publication.setTargetBethelIds(Set.of(bethel.getId()));
      publication.setAuthor(user);
      publication.setBethel(bethel);
      publication.setCreatedAt(LocalDateTime.now());
      publication.setUpdatedAt(publication.getCreatedAt());
      publication.setTimelineAt(publication.getCreatedAt());
      publicationId = publications.saveAndFlush(publication).getId();
      Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").claim("email", user.getEmail())
          .build();
      authentication = new JwtAuthenticationToken(jwt);
    });
  }

  @Test
  void uploadPersistsMetadataAndDeletionRemovesObjectThroughDurableQueue() {
    var uploaded = service.upload(publicationId, file(), authentication);
    var metadata = attachments.findById(uploaded.id()).orElseThrow();
    assertEquals("manual.pdf", metadata.getFilename());
    assertEquals(PDF.length, metadata.getSize());
    assertEquals(1, service.list(publicationId, false, authentication).size());
    assertArrayEquals(PDF,
        service.download(publicationId, uploaded.id(), false, authentication).getBody());
    assertEquals(0, queued(metadata.getObjectKey()));
    service.delete(publicationId, uploaded.id(), authentication);
    assertFalse(attachments.existsById(uploaded.id()));
    assertEquals(1, queued(metadata.getObjectKey()));
    assertArrayEquals(PDF, storage.download(metadata.getObjectKey()));
    cleanup.cleanup();
    assertEquals(0, queued(metadata.getObjectKey()));
    assertThrows(ResponseStatusException.class, () -> storage.download(metadata.getObjectKey()));
  }

  @Test
  void databaseRollbackLeavesUploadedObjectQueuedForCleanup() {
    assertThrows(IllegalStateException.class,
        () -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
          service.upload(publicationId, file(), authentication);
          throw new IllegalStateException("Falha simulada após upload e gravação de metadados.");
        }));
    assertEquals(0, attachments.findAllByPublicationIdOrderByIdAsc(publicationId).size());
    String key = jdbc.queryForObject(
        "SELECT object_key FROM storage_deletions " + "WHERE object_key LIKE ?", String.class,
        "documentos/publicacoes/" + publicationId + "/%");
    assertArrayEquals(PDF, storage.download(key));
    jdbc.update("UPDATE storage_deletions SET available_at = ? WHERE object_key = ?",
        LocalDateTime.now().minusSeconds(1), key);
    cleanup.cleanup();
    assertEquals(0, queued(key));
    assertThrows(ResponseStatusException.class, () -> storage.download(key));
  }

  private int queued(String key) {
    return jdbc.queryForObject("SELECT count(*) FROM storage_deletions WHERE object_key = ?",
        Integer.class, key);
  }

  private MockMultipartFile file() {
    return new MockMultipartFile("file", "manual.pdf", "application/pdf", PDF);
  }
}
