package com.bethelreport.backend.reportgeneration.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.reportgeneration.entities.GenerationRequest;
import com.bethelreport.backend.reportgeneration.entities.GenerationStatus;
import com.bethelreport.backend.reportgeneration.entities.GenerationType;
import com.bethelreport.backend.reportgeneration.mappers.GenerationMapperImpl;
import com.bethelreport.backend.reportgeneration.message.publishers.GenerationPublisher;
import com.bethelreport.backend.reportgeneration.repositories.GenerationRequestRepository;
import com.bethelreport.backend.reportgeneration.services.GenerationService;
import com.bethelreport.backend.security.config.SecurityConfig;
import com.bethelreport.backend.storage.MinioStorage;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import com.bethelreport.backend.users.services.AdmissionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(GenerationController.class)
@Import({SecurityConfig.class, AuthorizationService.class, GenerationService.class,
    GenerationMapperImpl.class})
class GenerationControllerTest {

  private static final String URL = "/api/bethels/10/report-generations";
  private static final String BODY = """
      {"type":"FINANCEIRO","periodStart":"2026-01-01","periodEnd":"2026-06-30"}
      """;

  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private GenerationRequestRepository repository;
  @MockitoBean
  private BethelRepository bethels;
  @MockitoBean
  private UserRepository users;
  @MockitoBean
  private PermissionRepository permissions;
  @MockitoBean
  private AdmissionService admission;
  @MockitoBean
  private JwtDecoder decoder;
  @MockitoBean
  private GenerationPublisher publisher;
  @MockitoBean
  private MinioStorage storage;

  private GenerationRequest entity;

  @BeforeEach
  void setUp() {
    when(admission.admittedUser(any())).thenReturn(true);
    Bethel bethel = new Bethel();
    bethel.setId(10L);
    User user = new User();
    user.setId(7L);
    Permission permission = new Permission();
    permission.setActive(true);
    permission.setRole(Role.CONSELHO_DE_BETHEL);
    permission.setBethel(bethel);
    when(permissions.findByUserEmail("council@test.com")).thenReturn(Optional.of(permission));
    when(bethels.findById(10L)).thenReturn(Optional.of(bethel));
    when(bethels.existsById(10L)).thenReturn(true);
    when(users.findByEmailIgnoreCase("council@test.com")).thenReturn(Optional.of(user));

    entity = new GenerationRequest();
    entity.setId(1L);
    entity.setBethel(bethel);
    entity.setRequester(user);
    entity.setGenerationType(GenerationType.FINANCEIRO);
    entity.setStatus(GenerationStatus.PENDING);
    entity.setPeriodStart(LocalDate.of(2026, 1, 1));
    entity.setPeriodEnd(LocalDate.of(2026, 6, 30));
    entity.setCreatedAt(LocalDateTime.of(2026, 11, 1, 12, 0));
    entity.setUpdatedAt(entity.getCreatedAt());
    when(repository.findByIdAndBethelId(1L, 10L)).thenReturn(Optional.of(entity));
    when(repository.save(any())).thenAnswer(invocation -> {
      GenerationRequest saved = invocation.getArgument(0);
      saved.setId(1L);
      return saved;
    });
  }

  private MockHttpServletRequestBuilder asCouncil(MockHttpServletRequestBuilder request) {
    return request.with(jwt().jwt(token -> token.claim("email", "council@test.com")));
  }

  @Test
  void createsGenerationRequestAndPublishesToQueue() throws Exception {
    mvc.perform(asCouncil(post(URL)).contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.bethelId").value(10)).andExpect(jsonPath("$.requesterId").value(7))
        .andExpect(jsonPath("$.type").value("FINANCEIRO"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.periodStart").value("2026-01-01"))
        .andExpect(jsonPath("$.periodEnd").value("2026-06-30"));
    verify(publisher).publish(any());
  }

  @Test
  void getsGenerationStatus() throws Exception {
    mvc.perform(asCouncil(get(URL + "/1"))).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void listsGenerationsWithPagination() throws Exception {
    when(repository.findByBethelId(eq(10L), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));
    mvc.perform(asCouncil(get(URL))).andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void downloadsCompletedGeneration() throws Exception {
    entity.setStatus(GenerationStatus.COMPLETED);
    entity.setStorageKey("reports/10/1.pdf");
    when(storage.download("reports/10/1.pdf")).thenReturn("pdf-content".getBytes());
    mvc.perform(asCouncil(get(URL + "/1/download"))).andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"report-1.pdf\""))
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));
  }

  @Test
  void rejectsDownloadOfPendingGeneration() throws Exception {
    mvc.perform(asCouncil(get(URL + "/1/download"))).andExpect(status().isConflict());
    verifyNoInteractions(storage);
  }

  @Test
  void rejectsInvalidPeriod() throws Exception {
    String body = """
        {"type":"FINANCEIRO","periodStart":"2026-06-30","periodEnd":"2026-01-01"}
        """;
    mvc.perform(asCouncil(post(URL)).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    verify(repository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"type\":\"INVALID\"}"})
  void rejectsMissingOrMalformedFields(String body) throws Exception {
    mvc.perform(asCouncil(post(URL)).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"page=-1", "size=0", "size=101"})
  void rejectsInvalidPagination(String query) throws Exception {
    mvc.perform(asCouncil(get(URL + "?" + query))).andExpect(status().isBadRequest());
    verifyNoInteractions(repository);
  }

  @Test
  void doesNotFindGenerationFromAnotherBethel() throws Exception {
    mvc.perform(asCouncil(get(URL + "/2"))).andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(strings = {"outsider", "member", "inactive", "missing"})
  void deniesOperationsWithoutActiveAdministrativeLink(String actor) throws Exception {
    Permission permission = new Permission();
    permission.setActive(!"inactive".equals(actor));
    permission.setRole("member".equals(actor) ? Role.MEMBRO_ATIVO : Role.CONSELHO_DE_BETHEL);
    Bethel actorBethel = new Bethel();
    actorBethel.setId("outsider".equals(actor) ? 20L : 10L);
    permission.setBethel(actorBethel);
    when(permissions.findByUserEmail("actor@test.com"))
        .thenReturn("missing".equals(actor) ? Optional.empty() : Optional.of(permission));
    for (MockHttpServletRequestBuilder request : List.of(get(URL), get(URL + "/1"), post(URL))) {
      mvc.perform(request.with(jwt().jwt(token -> token.claim("email", "actor@test.com")))
          .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }
    verifyNoInteractions(repository, bethels, users);
  }

  @Test
  void grandeConselhoCanAccessOtherBethel() throws Exception {
    Permission permission = new Permission();
    permission.setActive(true);
    permission.setRole(Role.GRANDE_CONSELHO);
    when(permissions.findByUserEmail("council@test.com")).thenReturn(Optional.of(permission));
    mvc.perform(asCouncil(get(URL + "/1"))).andExpect(status().isOk());
    mvc.perform(asCouncil(post(URL)).contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isCreated());
  }

  @Test
  void missingBethelReturnsNotFound() throws Exception {
    when(bethels.findById(10L)).thenReturn(Optional.empty());
    when(bethels.existsById(10L)).thenReturn(false);
    mvc.perform(asCouncil(post(URL)).contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isNotFound());
    mvc.perform(asCouncil(get(URL))).andExpect(status().isNotFound());
  }

  @Test
  void anonymousRequestsAreUnauthorized() throws Exception {
    for (MockHttpServletRequestBuilder request : List.of(get(URL), get(URL + "/1"), post(URL))) {
      mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(BODY))
          .andExpect(status().isUnauthorized());
    }
    verifyNoInteractions(repository);
  }
}
