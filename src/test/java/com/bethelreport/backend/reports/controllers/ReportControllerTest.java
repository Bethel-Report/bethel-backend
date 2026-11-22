package com.bethelreport.backend.reports.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.reports.dtos.ReportAnswerResponse;
import com.bethelreport.backend.reports.dtos.ReportResponse;
import com.bethelreport.backend.reports.entities.ReportStatus;
import com.bethelreport.backend.reports.services.ReportService;
import com.bethelreport.backend.security.config.SecurityConfig;
import com.bethelreport.backend.users.services.AdmissionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, AuthorizationService.class})
class ReportControllerTest {
  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private ReportService reportService;
  @MockitoBean
  private AdmissionService admissionService;
  @MockitoBean
  private JwtDecoder jwtDecoder;
  @MockitoBean
  private PermissionRepository permissionRepository;
  private static final String BASE = "/api/reports";

  @BeforeEach
  void setUpAuthorization() {
    when(permissionRepository.findByUserEmail("gc@example.com"))
        .thenReturn(Optional.of(permission(Role.GRANDE_CONSELHO, null)));
    when(permissionRepository.findByUserEmail("council@example.com"))
        .thenReturn(Optional.of(permission(Role.CONSELHO_DE_BETHEL, 10L)));
    when(permissionRepository.findByUserEmail("member@example.com"))
        .thenReturn(Optional.of(permission(Role.MEMBRO_ATIVO, 10L)));
  }

  @Test
  void createsReportWith201() throws Exception {
    when(reportService.create(any(), any())).thenReturn(reportResponse());
    mvc.perform(post(BASE).with(admittedJwt("council@example.com"))
        .contentType(MediaType.APPLICATION_JSON).content("{\"formId\":1}"))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("DRAFT"));
    verify(reportService).create(any(), any());
  }

  @Test
  void listsReports() throws Exception {
    when(reportService.findAll(any())).thenReturn(List.of(reportResponse()));
    mvc.perform(get(BASE).with(admittedJwt("gc@example.com"))).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].bethelName").value("Bethel 1"));
  }

  @Test
  void getsReportById() throws Exception {
    when(reportService.findById(eq(1L), any())).thenReturn(reportResponse());
    mvc.perform(get(BASE + "/1").with(admittedJwt("council@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answers[0].fieldLabel").value("Reuniões realizadas"));
  }

  @Test
  void savesAnswers() throws Exception {
    when(reportService.saveAnswers(eq(1L), any(), any())).thenReturn(reportResponse());
    mvc.perform(put(BASE + "/1/answers").with(admittedJwt("council@example.com"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"answers\":[{\"fieldId\":1,\"value\":\"42\"}]}")).andExpect(status().isOk());
    verify(reportService).saveAnswers(eq(1L), any(), any());
  }

  @Test
  void submitsReport() throws Exception {
    ReportResponse submitted = new ReportResponse(1L, 10L, "Bethel 1", 1L, "Relatório Anual 2026",
        ReportStatus.SUBMITTED, LocalDateTime.now(), null, null, List.of(), LocalDateTime.now(),
        LocalDateTime.now());
    when(reportService.submit(eq(1L), any())).thenReturn(submitted);
    mvc.perform(patch(BASE + "/1/submit").with(admittedJwt("council@example.com")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"));
  }

  @Test
  void reopensReportWithGrandeConselho() throws Exception {
    ReportResponse reopened = new ReportResponse(1L, 10L, "Bethel 1", 1L, "Relatório Anual 2026",
        ReportStatus.REOPENED, null, LocalDateTime.now(), LocalDateTime.now().plusDays(7),
        List.of(), LocalDateTime.now(), LocalDateTime.now());
    when(reportService.reopen(eq(1L), any())).thenReturn(reopened);
    mvc.perform(patch(BASE + "/1/reopen").with(admittedJwt("gc@example.com"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"reopenDeadline\":\"2026-12-31T23:59:59\"}")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REOPENED"));
  }

  @Test
  void conselhoCannotReopenReport() throws Exception {
    mvc.perform(patch(BASE + "/1/reopen").with(admittedJwt("council@example.com"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"reopenDeadline\":\"2026-12-31T23:59:59\"}")).andExpect(status().isForbidden());
    verifyNoInteractions(reportService);
  }

  @Test
  void memberCannotReopenReport() throws Exception {
    mvc.perform(patch(BASE + "/1/reopen").with(admittedJwt("member@example.com"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"reopenDeadline\":\"2026-12-31T23:59:59\"}")).andExpect(status().isForbidden());
    verifyNoInteractions(reportService);
  }

  @Test
  void unauthenticatedCannotAccessReports() throws Exception {
    mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    verifyNoInteractions(reportService);
  }

  @Test
  void rejectsInvalidCreateRequest() throws Exception {
    mvc.perform(post(BASE).with(admittedJwt("council@example.com"))
        .contentType(MediaType.APPLICATION_JSON).content("{\"formId\":null}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(reportService);
  }

  @Test
  void rejectsInvalidSaveAnswersRequest() throws Exception {
    mvc.perform(put(BASE + "/1/answers").with(admittedJwt("council@example.com"))
        .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[]}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(reportService);
  }

  private Permission permission(Role role, Long bethelId) {
    Permission permission = new Permission();
    permission.setRole(role);
    if (bethelId != null) {
      Bethel bethel = new Bethel();
      bethel.setId(bethelId);
      permission.setBethel(bethel);
    }
    return permission;
  }

  private RequestPostProcessor admittedJwt(String email) {
    when(admissionService.admittedUser(any(Jwt.class))).thenReturn(true);
    return jwt().jwt(token -> token.claim("email", email));
  }

  private ReportResponse reportResponse() {
    ReportAnswerResponse answer = new ReportAnswerResponse(1L, 1L, "Reuniões realizadas",
        com.bethelreport.backend.forms.entities.FormFieldType.NUMBER, "42", LocalDateTime.now());
    return new ReportResponse(1L, 10L, "Bethel 1", 1L, "Relatório Anual 2026", ReportStatus.DRAFT,
        null, null, null, List.of(answer), LocalDateTime.now(), LocalDateTime.now());
  }
}
