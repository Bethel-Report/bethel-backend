package com.bethelreport.backend.controllers;

import com.bethelreport.backend.config.SecurityConfig;
import com.bethelreport.backend.dtos.bethel.BethelResponse;
import com.bethelreport.backend.services.AdmissionService;
import com.bethelreport.backend.services.BethelService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BethelController.class)
@Import(SecurityConfig.class)
class BethelControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean BethelService service;
  @MockitoBean AdmissionService admissionService;
  @MockitoBean JwtDecoder jwtDecoder;

  private static final String BASE = "/api/bethels";

  private String validJson() {
    return """
        {"number":12,"name":"Lírios do Vale","foundationDate":"2020-05-10",
         "city":"Alexandria","address":"Rua Principal","email":"bethel@example.com",
         "sponsoringLodge":"Loja União"}
        """;
  }

  private BethelResponse response(boolean active) {
    return new BethelResponse(1L, 12, "Lírios do Vale", LocalDate.of(2020, 5, 10),
        "Alexandria", "Rua Principal", "bethel@example.com", "Loja União", active);
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor admittedJwt() {
    when(admissionService.admittedUser(any(Jwt.class))).thenReturn(true);
    return jwt();
  }

  @Test
  void createsBethelWith201() throws Exception {
    when(service.create(any())).thenReturn(response(true));
    mvc.perform(post(BASE).with(admittedJwt()).contentType(MediaType.APPLICATION_JSON)
        .content(validJson()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.number").value(12))
        .andExpect(jsonPath("$.active").value(true));
    verify(service).create(any());
  }

  @Test
  void listsBethels() throws Exception {
    when(service.findAll(null)).thenReturn(List.of(response(true)));
    mvc.perform(get(BASE).with(admittedJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].number").value(12));
  }

  @Test
  void filtersBethelsByName() throws Exception {
    when(service.findAll("Lírios")).thenReturn(List.of(response(true)));
    mvc.perform(get(BASE).param("name", "Lírios").with(admittedJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Lírios do Vale"));
    verify(service).findAll("Lírios");
  }

  @Test
  void findsBethelById() throws Exception {
    when(service.findById(1L)).thenReturn(response(true));
    mvc.perform(get(BASE + "/1").with(admittedJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void updatesBethel() throws Exception {
    when(service.update(eq(1L), any())).thenReturn(response(true));
    mvc.perform(put(BASE + "/1").with(admittedJwt())
        .contentType(MediaType.APPLICATION_JSON).content(validJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(12));
    verify(service).update(eq(1L), any());
  }

  @Test
  void activatesBethel() throws Exception {
    when(service.activate(1L)).thenReturn(response(true));
    mvc.perform(patch(BASE + "/1/activate").with(admittedJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void deactivatesBethel() throws Exception {
    when(service.deactivate(1L)).thenReturn(response(false));
    mvc.perform(patch(BASE + "/1/deactivate").with(admittedJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void rejectsMissingRequiredFieldsOnCreate() throws Exception {
    mvc.perform(post(BASE).with(admittedJwt()).contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void rejectsInvalidNumberAndEmailOnCreate() throws Exception {
    mvc.perform(post(BASE).with(admittedJwt()).contentType(MediaType.APPLICATION_JSON)
        .content(validJson().replace("\"number\":12", "\"number\":0")
            .replace("bethel@example.com", "invalido")))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void rejectsFutureFoundationDateOnCreate() throws Exception {
    mvc.perform(post(BASE).with(admittedJwt()).contentType(MediaType.APPLICATION_JSON)
        .content(validJson().replace("2020-05-10", "2999-01-01")))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void rejectsBlankRequiredFieldOnUpdate() throws Exception {
    mvc.perform(put(BASE + "/1").with(admittedJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson().replace("Lírios do Vale", "  ")))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void rejectsInvalidEmailOnUpdate() throws Exception {
    mvc.perform(put(BASE + "/1").with(admittedJwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson().replace("bethel@example.com", "invalido")))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void requiresAuthentication() throws Exception {
    mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    verifyNoInteractions(service);
  }

  @Test
  void rejectsUserNotAdmitted() throws Exception {
    when(admissionService.admittedUser(any(Jwt.class))).thenReturn(false);
    mvc.perform(get(BASE).with(jwt())).andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void rejectsNotAdmittedUserOnWrite() throws Exception {
    when(admissionService.admittedUser(any(Jwt.class))).thenReturn(false);
    mvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
        .content(validJson())).andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }
}
