package com.bethelreport.backend.publications.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.security.config.SecurityConfig;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationAttachment;
import com.bethelreport.backend.publications.entities.PublicationAudience;
import com.bethelreport.backend.publications.entities.PublicationType;
import com.bethelreport.backend.publications.mappers.PublicationMapperImpl;
import com.bethelreport.backend.publications.repositories.PublicationAttachmentRepository;
import com.bethelreport.backend.publications.repositories.PublicationRepository;
import com.bethelreport.backend.publications.services.PublicationAccess;
import com.bethelreport.backend.publications.services.PublicationAttachmentService;
import com.bethelreport.backend.publications.services.PublicationDateRules;
import com.bethelreport.backend.publications.services.PublicationDeletionService;
import com.bethelreport.backend.publications.services.PublicationRules;
import com.bethelreport.backend.publications.services.PublicationService;
import com.bethelreport.backend.storage.MinioStorage;
import com.bethelreport.backend.storage.StorageCleanup;
import com.bethelreport.backend.storage.StorageDeletionQueue;
import com.bethelreport.backend.storage.UploadPolicy;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.services.AdmissionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest({PublicationController.class, PublicationAttachmentController.class})
@Import({SecurityConfig.class, PublicationService.class, PublicationAccess.class,
    PublicationRules.class, PublicationDateRules.class, PublicationDeletionService.class,
    PublicationAttachmentService.class, UploadPolicy.class, PublicationMapperImpl.class})
@TestPropertySource(properties = {"storage.upload.allowed-extensions=pdf,png,mp4",
    "storage.upload.max-size=5MB"})
class PublicationControllerTest {
  @Autowired
  private MockMvc mvc;
  @Autowired
  private ObjectMapper mapper;
  @MockitoBean
  private PublicationRepository publications;
  @MockitoBean
  private PublicationAttachmentRepository attachments;
  @MockitoBean
  private PermissionRepository permissions;
  @MockitoBean
  private BethelRepository bethels;
  @MockitoBean
  private MinioStorage storage;
  @MockitoBean
  private StorageCleanup cleanup;
  @MockitoBean
  private StorageDeletionQueue deletionQueue;
  @MockitoBean
  private AdmissionService admission;
  @MockitoBean
  private JwtDecoder jwtDecoder;
  private Permission actor;
  private Publication publication;
  private Bethel bethel;
  private PublicationAttachment attachment;

  @BeforeEach
  void setUp() {
    when(admission.admittedUser(any())).thenReturn(true);
    User user = new User();
    user.setId(7L);
    user.setActive(true);
    bethel = new Bethel();
    bethel.setId(10L);
    actor = new Permission();
    actor.setRole(Role.CONSELHO_DE_BETHEL);
    actor.setUser(user);
    actor.setBethel(bethel);
    when(permissions.findByUserEmail("actor@example.com")).thenReturn(Optional.of(actor));
    when(permissions.findByUserId(7L)).thenReturn(Optional.of(actor));
    when(bethels.findById(10L)).thenReturn(Optional.of(bethel));
    publication = new Publication();
    publication.setId(1L);
    publication.setTitle("Original");
    publication.setContent("Content");
    publication.setType(PublicationType.ACTIVITY);
    publication.setAudience(PublicationAudience.BETHELS);
    publication.setTargetBethelIds(Set.of(10L));
    publication.setActivityDate(LocalDate.now().minusDays(1));
    publication.setBethel(bethel);
    publication.setAuthor(user);
    publication.setCreatedAt(LocalDateTime.of(2026, 10, 1, 10, 0));
    publication.setUpdatedAt(publication.getCreatedAt());
    when(publications.findById(1L)).thenReturn(Optional.of(publication));
    when(publications.findForUpdate(1L)).thenReturn(Optional.of(publication));
    when(publications.save(any())).thenAnswer(call -> {
      Publication entity = call.getArgument(0);
      entity.setId(1L);
      return entity;
    });
    attachment = new PublicationAttachment();
    attachment.setId(2L);
    attachment.setPublication(publication);
    attachment.setFilename("manual.pdf");
    attachment.setContentType("application/pdf");
    attachment.setObjectKey("documentos/publicacoes/1/random");
    attachment.setSize(10);
    attachment.setCreatedAt(LocalDateTime.now());
    when(attachments.findByIdAndPublicationId(2L, 1L)).thenReturn(Optional.of(attachment));
    when(attachments.findAllByPublicationIdOrderByIdAsc(1L)).thenReturn(List.of(attachment));
    when(attachments.saveAndFlush(any())).thenAnswer(call -> {
      PublicationAttachment entity = call.getArgument(0);
      entity.setId(2L);
      return entity;
    });
  }

  private RequestPostProcessor token() {
    return jwt().jwt(value -> value.claim("email", "actor@example.com"));
  }

  private Map<String, Object> activity() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", " Activity ");
    body.put("content", "Description");
    body.put("type", "ACTIVITY");
    body.put("activityDate", LocalDate.now().minusDays(1).toString());
    body.put("bethelId", 10);
    body.put("audience", "BETHELS");
    body.put("targetBethelIds", List.of(10));
    return body;
  }

  private Map<String, Object> announcement() {
    Map<String, Object> body = activity();
    body.put("type", "ANNOUNCEMENT");
    body.remove("activityDate");
    body.put("displayFrom", LocalDateTime.now().minusHours(1).toString());
    body.put("displayUntil", LocalDateTime.now().plusHours(1).toString());
    return body;
  }

  @Test
  void createsActivityWithAuthenticatedAuthorAndRealizationDate() throws Exception {
    var body = activity();
    body.put("authorId", 999);
    mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.authorId").value(7)).andExpect(jsonPath("$.title").value("Activity"))
        .andExpect(jsonPath("$.activityDate").value(LocalDate.now().minusDays(1).toString()));
  }

  @Test
  void grandeConselhoCanTargetAllBethelsMultipleBethelsOrSpecificUsers() throws Exception {
    actor.setRole(Role.GRANDE_CONSELHO);
    Bethel other = new Bethel();
    other.setId(20L);
    when(bethels.findById(20L)).thenReturn(Optional.of(other));
    for (String audience : List.of("ALL_BETHELS", "BETHELS", "USERS")) {
      var body = announcement();
      body.remove("bethelId");
      body.remove("targetBethelIds");
      body.put("audience", audience);
      if ("BETHELS".equals(audience)) {
        body.put("targetBethelIds", List.of(10, 20));
      } else if ("USERS".equals(audience)) {
        body.put("targetUserIds", List.of(7));
      }
      mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(body))).andExpect(status().isCreated())
          .andExpect(jsonPath("$.audience").value(audience))
          .andExpect(jsonPath("$.targetBethelIds").doesNotExist())
          .andExpect(jsonPath("$.targetUserIds").doesNotExist());
    }
  }

  @Test
  void councilCannotPublishStatewideOrToOtherBethel() throws Exception {
    var body = announcement();
    body.remove("bethelId");
    body.remove("targetBethelIds");
    body.put("audience", "ALL_BETHELS");
    mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isForbidden());
    body = announcement();
    body.put("bethelId", 20);
    body.put("targetBethelIds", List.of(20));
    mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isForbidden());
  }

  @Test
  void councilCanSelectOwnUsersButNotUsersFromAnotherBethel() throws Exception {
    var body = announcement();
    body.put("audience", "USERS");
    body.remove("targetBethelIds");
    body.put("targetUserIds", List.of(7));
    mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isCreated());
    Bethel other = new Bethel();
    other.setId(20L);
    Permission foreign = new Permission();
    foreign.setUser(actor.getUser());
    foreign.setRole(Role.MEMBRO_ATIVO);
    foreign.setBethel(other);
    when(permissions.findByUserId(8L)).thenReturn(Optional.of(foreign));
    body.put("targetUserIds", List.of(8));
    mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isForbidden());
  }

  @Test
  void nonAdministrativeMembersCanReadButCannotCreateEditDeleteOrUpload() throws Exception {
    for (Role role : List.of(Role.MEMBRO_ATIVO, Role.MEMBRO_DE_MAIORIDADE)) {
      actor.setRole(role);
      mvc.perform(get("/api/publications/1").with(token())).andExpect(status().isOk());
      mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(activity()))).andExpect(status().isForbidden());
      mvc.perform(put("/api/publications/1").with(token()).contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(activity()))).andExpect(status().isForbidden());
      mvc.perform(delete("/api/publications/1").with(token())).andExpect(status().isForbidden());
      mvc.perform(multipart("/api/publications/1/attachments").file(pdf()).with(token()))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void unselectedUsersCannotReadPublicationsOrAttachmentsEvenByDirectId() throws Exception {
    publication.setAudience(PublicationAudience.USERS);
    publication.setTargetBethelIds(Set.of());
    publication.setTargetUserIds(Set.of(99L));
    mvc.perform(get("/api/publications/1").with(token())).andExpect(status().isForbidden());
    mvc.perform(get("/api/publications/1/attachments").with(token()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/publications/1/attachments/2").with(token()))
        .andExpect(status().isForbidden());
    verify(storage, never()).download(any());
    publication.setTargetUserIds(Set.of(7L));
    mvc.perform(get("/api/publications/1").with(token())).andExpect(status().isOk());
  }

  @Test
  void futureAndExpiredPublicationsAreHiddenIncludingTheirFiles() throws Exception {
    publication.setDisplayFrom(LocalDateTime.now().plusHours(1));
    mvc.perform(get("/api/publications/1").with(token())).andExpect(status().isForbidden());
    publication.setDisplayFrom(LocalDateTime.now().minusHours(2));
    publication.setDisplayUntil(LocalDateTime.now().minusHours(1));
    mvc.perform(get("/api/publications/1/attachments/2").with(token()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/publications/1?management=true").with(token()))
        .andExpect(status().isOk());
    actor.setRole(Role.MEMBRO_ATIVO);
    mvc.perform(get("/api/publications/1?management=true").with(token()))
        .andExpect(status().isForbidden());
  }

  @Test
  void councilCannotManageStatewidePublicationEvenWhenTargetedAsReader() throws Exception {
    publication.setType(PublicationType.GUIDANCE);
    publication.setActivityDate(null);
    publication.setBethel(null);
    mvc.perform(get("/api/publications/1").with(token())).andExpect(status().isOk());
    mvc.perform(get("/api/publications/1?management=true").with(token()))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/publications/1").with(token())).andExpect(status().isForbidden());
  }

  @Test
  void localUserTargetCannotBeReadAfterMemberMovesToAnotherBethel() throws Exception {
    publication.setType(PublicationType.GUIDANCE);
    publication.setActivityDate(null);
    publication.setAudience(PublicationAudience.USERS);
    publication.setTargetBethelIds(Set.of());
    publication.setTargetUserIds(Set.of(7L));
    Bethel other = new Bethel();
    other.setId(20L);
    actor.setBethel(other);
    actor.setRole(Role.MEMBRO_ATIVO);
    mvc.perform(get("/api/publications/1").with(token())).andExpect(status().isForbidden());
    mvc.perform(get("/api/publications/1/attachments/2").with(token()))
        .andExpect(status().isForbidden());
    publication.setBethel(null);
    mvc.perform(get("/api/publications/1").with(token())).andExpect(status().isOk());
  }

  @Test
  void updatePreservesAuthorAndCreationTimeAndCannotChangeOrganizationalScope() throws Exception {
    mvc.perform(put("/api/publications/1").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(activity()))).andExpect(status().isOk())
        .andExpect(jsonPath("$.authorId").value(7))
        .andExpect(jsonPath("$.createdAt").value("2026-10-01T10:00:00"));
    var body = activity();
    body.put("bethelId", 20);
    body.put("targetBethelIds", List.of(20));
    mvc.perform(put("/api/publications/1").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isForbidden());
  }

  @Test
  void feedSupportsBothChronologicalOrdersAndBoundsPagination() throws Exception {
    when(
        publications.findFeed(anyBoolean(), anyBoolean(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(publication)));
    for (String order : List.of("ASC", "DESC")) {
      mvc.perform(get("/api/publications?order=" + order + "&page=1&size=5").with(token()))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(1));
      verify(publications)
          .findFeed(eq(false), eq(false), eq(10L), eq(7L), any(), isNull(), isNull(),
              org.mockito.ArgumentMatchers.argThat((Pageable page) -> page.getPageNumber() == 1
                  && page.getPageSize() == 5
                  && page.getSort().getOrderFor("timelineAt").getDirection().name().equals(order)));
    }
    for (String query : List.of("page=-1", "size=0", "size=101", "order=INVALID")) {
      mvc.perform(get("/api/publications?" + query).with(token()))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(get("/api/publications?type=ANNOUNCEMENT").with(token()))
        .andExpect(status().isOk());
    verify(publications).findFeed(eq(false), eq(false), eq(10L), eq(7L), any(), isNull(),
        eq(PublicationType.ANNOUNCEMENT), any());
    actor.setRole(Role.MEMBRO_ATIVO);
    mvc.perform(get("/api/publications?management=true").with(token()))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsMissingFieldsInvalidDatesAndMismatchedAudience() throws Exception {
    for (String field : List.of("title", "content", "type", "activityDate", "audience",
        "bethelId")) {
      var body = activity();
      body.remove(field);
      mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(body))).andExpect(status().is4xxClientError());
    }
    var body = activity();
    body.put("activityDate", LocalDate.now().plusDays(1).toString());
    mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
    body = activity();
    body.put("targetUserIds", List.of(7));
    mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
    body = announcement();
    body.put("displayUntil", body.get("displayFrom"));
    mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
    body.remove("displayUntil");
    mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
  }

  @Test
  void inactiveUserPermissionAndBethelAreRejected() throws Exception {
    actor.setActive(false);
    mvc.perform(get("/api/publications/1").with(token())).andExpect(status().isForbidden());
    actor.setActive(true);
    actor.getUser().setActive(false);
    mvc.perform(get("/api/publications/1").with(token())).andExpect(status().isForbidden());
    actor.getUser().setActive(true);
    actor.setRole(Role.GRANDE_CONSELHO);
    actor.setBethel(null);
    bethel.setActive(false);
    mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(activity()))).andExpect(status().isBadRequest());
  }

  @Test
  void responsesNeverExposeDistributionTargets() throws Exception {
    when(
        publications.findFeed(anyBoolean(), anyBoolean(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(publication)));
    for (boolean management : List.of(false, true)) {
      mvc.perform(get("/api/publications/1?management=" + management).with(token()))
          .andExpect(status().isOk()).andExpect(jsonPath("$.authorId").value(7))
          .andExpect(jsonPath("$.bethelId").value(10))
          .andExpect(jsonPath("$.targetBethelIds").doesNotExist())
          .andExpect(jsonPath("$.targetUserIds").doesNotExist());
      mvc.perform(get("/api/publications?management=" + management).with(token()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].targetBethelIds").doesNotExist())
          .andExpect(jsonPath("$.content[0].targetUserIds").doesNotExist());
    }
    mvc.perform(put("/api/publications/1").with(token()).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(activity()))).andExpect(status().isOk())
        .andExpect(jsonPath("$.targetBethelIds").doesNotExist())
        .andExpect(jsonPath("$.targetUserIds").doesNotExist());
  }

  @Test
  void inactiveAssociatedBethelBlocksAllPublicationAndAttachmentOperations() throws Exception {
    bethel.setActive(false);
    for (Role role : List.of(Role.MEMBRO_ATIVO, Role.MEMBRO_DE_MAIORIDADE, Role.CONSELHO_DE_BETHEL,
        Role.GRANDE_CONSELHO)) {
      actor.setRole(role);
      for (String route : List.of("/api/publications", "/api/publications?management=true",
          "/api/publications/1", "/api/publications/1?management=true",
          "/api/publications/1/attachments", "/api/publications/1/attachments/2")) {
        mvc.perform(get(route).with(token())).andExpect(status().isForbidden());
      }
      mvc.perform(post("/api/publications").with(token()).contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(activity()))).andExpect(status().isForbidden());
      mvc.perform(put("/api/publications/1").with(token()).contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(activity()))).andExpect(status().isForbidden());
      mvc.perform(delete("/api/publications/1").with(token())).andExpect(status().isForbidden());
      mvc.perform(multipart("/api/publications/1/attachments").file(pdf()).with(token()))
          .andExpect(status().isForbidden());
      mvc.perform(delete("/api/publications/1/attachments/2").with(token()))
          .andExpect(status().isForbidden());
    }
    verify(storage, never()).download(any());
    verify(storage, never()).upload(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
    verify(publications, never()).findFeed(anyBoolean(), anyBoolean(), any(), any(), any(), any(),
        any(), any());
  }

  @Test
  void missingResourcesAndAnonymousRequestsReturnExpectedStatus() throws Exception {
    mvc.perform(get("/api/publications")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/publications/99").with(token())).andExpect(status().isNotFound());
    mvc.perform(get("/api/publications/1/attachments/99").with(token()))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/publications/99").with(token())).andExpect(status().isNotFound());
  }

  private MockMultipartFile pdf() {
    return new MockMultipartFile("file", "manual.pdf", "application/pdf",
        "%PDF-1.7 file".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
  }

  @Test
  void uploadsDocumentWithServerGeneratedKeyAndDownloadsOnlyAfterAuthorization() throws Exception {
    mvc.perform(multipart("/api/publications/1/attachments").file(pdf()).with(token()))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.filename").value("manual.pdf"))
        .andExpect(jsonPath("$.objectKey").doesNotExist());
    verify(cleanup)
        .registerUpload(org.mockito.ArgumentMatchers.startsWith("documentos/publicacoes/1/"));
    verify(storage).upload(org.mockito.ArgumentMatchers.startsWith("documentos/publicacoes/1/"),
        any(), eq(13L), eq("application/pdf"));
    when(storage.download(attachment.getObjectKey())).thenReturn(new byte[]{1, 2});
    mvc.perform(get("/api/publications/1/attachments/2").with(token())).andExpect(status().isOk());
  }

  @Test
  void uploadRejectsEmptyUnsupportedSpoofedAndOversizedFiles() throws Exception {
    for (MockMultipartFile file : List.of(
        new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]),
        new MockMultipartFile("file", "script.html", "text/html", new byte[]{1}),
        new MockMultipartFile("file", "fake.pdf", "application/pdf", new byte[]{1}),
        new MockMultipartFile("file", "large.pdf", "application/pdf",
            new byte[5 * 1024 * 1024 + 1]))) {
      mvc.perform(multipart("/api/publications/1/attachments").file(file).with(token()))
          .andExpect(status().is4xxClientError());
    }
    verify(storage, never()).upload(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
  }

  @Test
  void deletionQueuesPhysicalCleanupInsideDatabaseTransaction() throws Exception {
    mvc.perform(delete("/api/publications/1").with(token())).andExpect(status().isNoContent());
    verify(deletionQueue).enqueueDeletion(attachment.getObjectKey());
    verify(attachments).deleteAll(List.of(attachment));
    verify(publications).delete(publication);
    verify(storage, never()).delete(any());
  }

  @Test
  void deletingAnAttachmentKeepsPublicationAndQueuesCleanup() throws Exception {
    mvc.perform(delete("/api/publications/1/attachments/2").with(token()))
        .andExpect(status().isNoContent());
    verify(cleanup).enqueueDeletion(attachment.getObjectKey());
    verify(attachments).delete(attachment);
    verify(publications, never()).delete(any());
  }

  @Test
  void failedUploadDoesNotCreateMetadataOrCancelOrphanCleanup() throws Exception {
    org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
        .when(storage).upload(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
    mvc.perform(multipart("/api/publications/1/attachments").file(pdf()).with(token()))
        .andExpect(status().isServiceUnavailable());
    verify(attachments, never()).saveAndFlush(any());
    verify(cleanup, never()).cancelUploadCleanup(any());
  }
}
