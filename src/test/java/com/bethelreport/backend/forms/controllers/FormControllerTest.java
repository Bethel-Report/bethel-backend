package com.bethelreport.backend.forms.controllers;

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
import com.bethelreport.backend.forms.dtos.FormFieldResponse;
import com.bethelreport.backend.forms.dtos.FormResponse;
import com.bethelreport.backend.forms.entities.FormFieldType;
import com.bethelreport.backend.forms.entities.FormStatus;
import com.bethelreport.backend.forms.services.FormService;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.security.config.SecurityConfig;
import com.bethelreport.backend.users.services.AdmissionService;
import java.time.LocalDate;
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

@WebMvcTest(FormController.class)
@Import({SecurityConfig.class, AuthorizationService.class})
class FormControllerTest {
  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private FormService formService;
  @MockitoBean
  private AdmissionService admissionService;
  @MockitoBean
  private JwtDecoder jwtDecoder;
  @MockitoBean
  private PermissionRepository permissionRepository;
  private static final String BASE = "/api/forms";

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
  void createsFormWith201() throws Exception {
    when(formService.create(any())).thenReturn(formResponse());
    mvc.perform(post(BASE).with(admittedJwt()).contentType(MediaType.APPLICATION_JSON)
        .content(validCreateJson())).andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Relatório Anual 2026"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
    verify(formService).create(any());
  }

  @Test
  void listsFormsWithGrandeConselho() throws Exception {
    when(formService.findAll()).thenReturn(List.of(formResponse()));
    mvc.perform(get(BASE).with(admittedJwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Relatório Anual 2026"));
  }

  @Test
  void getsFormByIdWithGrandeConselho() throws Exception {
    when(formService.findById(1L)).thenReturn(formResponse());
    mvc.perform(get(BASE + "/1").with(admittedJwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$.fields[0].label").value("Reuniões realizadas"));
  }

  @Test
  void updatesFormWithGrandeConselho() throws Exception {
    when(formService.update(eq(1L), any())).thenReturn(formResponse());
    mvc.perform(put(BASE + "/1").with(admittedJwt()).contentType(MediaType.APPLICATION_JSON)
        .content(validUpdateJson())).andExpect(status().isOk());
    verify(formService).update(eq(1L), any());
  }

  @Test
  void clonesFormWith201() throws Exception {
    when(formService.clone(eq(1L), any())).thenReturn(formResponse());
    mvc.perform(post(BASE + "/1/clone").with(admittedJwt()).contentType(MediaType.APPLICATION_JSON)
        .content(validCloneJson())).andExpect(status().isCreated());
    verify(formService).clone(eq(1L), any());
  }

  @Test
  void publishesFormWithGrandeConselho() throws Exception {
    FormResponse published = new FormResponse(1L, "Relatório Anual 2026", null, 2026,
        FormStatus.PUBLISHED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), null,
        List.of(), LocalDateTime.now(), LocalDateTime.now());
    when(formService.publish(1L)).thenReturn(published);
    mvc.perform(patch(BASE + "/1/publish").with(admittedJwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));
  }

  @Test
  void closesFormWithGrandeConselho() throws Exception {
    FormResponse closed = new FormResponse(1L, "Relatório Anual 2026", null, 2026,
        FormStatus.CLOSED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), null, List.of(),
        LocalDateTime.now(), LocalDateTime.now());
    when(formService.close(1L)).thenReturn(closed);
    mvc.perform(patch(BASE + "/1/close").with(admittedJwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  void conselhoCannotAccessForms() throws Exception {
    mvc.perform(get(BASE).with(admittedJwt("council@example.com")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(formService);
  }

  @Test
  void memberCannotAccessForms() throws Exception {
    mvc.perform(get(BASE).with(admittedJwt("member@example.com")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(formService);
  }

  @Test
  void unauthenticatedCannotAccessForms() throws Exception {
    mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    verifyNoInteractions(formService);
  }

  @Test
  void rejectsInvalidCreateRequest() throws Exception {
    String invalid = """
        {"name":"","referenceYear":null,"fields":[]}
        """;
    mvc.perform(post(BASE).with(admittedJwt()).contentType(MediaType.APPLICATION_JSON)
        .content(invalid)).andExpect(status().isBadRequest());
    verifyNoInteractions(formService);
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

  private RequestPostProcessor admittedJwt() {
    return admittedJwt("gc@example.com");
  }

  private RequestPostProcessor admittedJwt(String email) {
    when(admissionService.admittedUser(any(Jwt.class))).thenReturn(true);
    return jwt().jwt(token -> token.claim("email", email));
  }

  private FormResponse formResponse() {
    FormFieldResponse field =
        new FormFieldResponse(1L, "Reuniões realizadas", FormFieldType.NUMBER, true, 1, List.of());
    return new FormResponse(1L, "Relatório Anual 2026", null, 2026, FormStatus.DRAFT,
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), null, List.of(field),
        LocalDateTime.now(), LocalDateTime.now());
  }

  private String validCreateJson() {
    return """
        {"name":"Relatório Anual 2026","referenceYear":2026,
         "openingDate":"2026-01-01","closingDate":"2026-03-31",
         "fields":[{"label":"Reuniões realizadas","type":"NUMBER",
         "required":true,"displayOrder":1}]}
        """;
  }

  private String validUpdateJson() {
    return """
        {"name":"Relatório Anual 2026 Atualizado","referenceYear":2026,
         "openingDate":"2026-01-01","closingDate":"2026-03-31",
         "fields":[{"label":"Reuniões realizadas","type":"NUMBER",
         "required":true,"displayOrder":1}]}
        """;
  }

  private String validCloneJson() {
    return """
        {"name":"Relatório Anual 2027","referenceYear":2027,
         "openingDate":"2027-01-01","closingDate":"2027-03-31"}
        """;
  }
}
