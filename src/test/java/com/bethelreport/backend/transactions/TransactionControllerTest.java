package com.bethelreport.backend.transactions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.security.config.SecurityConfig;
import com.bethelreport.backend.transactions.controllers.TransactionController;
import com.bethelreport.backend.transactions.entities.FinancialTransaction;
import com.bethelreport.backend.transactions.entities.TransactionType;
import com.bethelreport.backend.transactions.repositories.TransactionRepository;
import com.bethelreport.backend.transactions.services.TransactionService;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import com.bethelreport.backend.users.services.AdmissionService;
import java.math.BigDecimal;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, AuthorizationService.class, TransactionService.class})
class TransactionControllerTest {
  private static final String URL = "/api/bethels/10/transactions";
  private static final String BODY = """
      {"type":"RECEITA","amount":125.50,"transactionDate":"2026-11-01",
       "description":" Contribuição "}
      """;

  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private TransactionRepository repository;
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

  private FinancialTransaction entity;

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
    entity = new FinancialTransaction();
    entity.setId(1L);
    entity.setBethel(bethel);
    entity.setAuthor(user);
    entity.setType(TransactionType.RECEITA);
    entity.setAmount(new BigDecimal("125.50"));
    entity.setTransactionDate(LocalDate.of(2026, 11, 1));
    entity.setDescription("Contribuição");
    entity.setCreatedAt(LocalDateTime.of(2026, 11, 1, 12, 0));
    entity.setUpdatedAt(entity.getCreatedAt());
    when(repository.findByIdAndBethelId(1L, 10L)).thenReturn(Optional.of(entity));
    when(repository.save(any())).thenAnswer(invocation -> {
      FinancialTransaction saved = invocation.getArgument(0);
      saved.setId(1L);
      return saved;
    });
  }

  private MockHttpServletRequestBuilder asCouncil(MockHttpServletRequestBuilder request) {
    return request.with(jwt().jwt(token -> token.claim("email", "council@test.com")));
  }

  @Test
  void createsWithBethelAndAuthenticatedAuthor() throws Exception {
    mvc.perform(asCouncil(post(URL)).contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.bethelId").value(10)).andExpect(jsonPath("$.userId").value(7))
        .andExpect(jsonPath("$.amount").value(125.50))
        .andExpect(jsonPath("$.description").value("Contribuição"));
  }

  @Test
  void readsUpdatesAndDeletes() throws Exception {
    mvc.perform(asCouncil(get(URL + "/1"))).andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("RECEITA"));
    mvc.perform(asCouncil(put(URL + "/1")).contentType(MediaType.APPLICATION_JSON)
        .content(BODY.replace("RECEITA", "DESPESA"))).andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("DESPESA")).andExpect(jsonPath("$.userId").value(7));
    mvc.perform(asCouncil(delete(URL + "/1"))).andExpect(status().isNoContent());
    verify(repository).delete(entity);
  }

  @Test
  void listsWithPaginationAndFilters() throws Exception {
    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));
    mvc.perform(asCouncil(get(URL)).param("type", "RECEITA").param("startDate", "2026-11-01")
        .param("endDate", "2026-11-01")).andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.totalElements").value(1));
    verify(repository).findAll(any(Specification.class), eq(
        org.springframework.data.domain.PageRequest.of(0, 20, org.springframework.data.domain.Sort
            .by(org.springframework.data.domain.Sort.Direction.DESC, "transactionDate", "id"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"type\":\"INVALID\"}",
      "{\"type\":\"RECEITA\",\"amount\":1,\"transactionDate\":\"2026-02-30\",\"description\":\"x\"}"})
  void rejectsMissingOrMalformedFields(String body) throws Exception {
    mvc.perform(asCouncil(post(URL)).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "0.001", "1.001", "100000000000000000"})
  void rejectsInvalidAmountsOnCreateAndUpdate(String amount) throws Exception {
    for (MockHttpServletRequestBuilder request : List.of(post(URL), put(URL + "/1"))) {
      mvc.perform(asCouncil(request).contentType(MediaType.APPLICATION_JSON)
          .content(BODY.replace("125.50", amount))).andExpect(status().isBadRequest());
    }
    verifyNoInteractions(repository);
  }

  @Test
  void rejectsBlankAndLongDescriptions() throws Exception {
    for (String description : List.of("   ", "a".repeat(2001))) {
      mvc.perform(asCouncil(post(URL)).contentType(MediaType.APPLICATION_JSON)
          .content(BODY.replace(" Contribuição ", description))).andExpect(status().isBadRequest());
    }
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"startDate=2026-11-02&endDate=2026-11-01", "page=-1", "size=0",
      "size=101", "type=INVALID", "startDate=bad-date"})
  void rejectsInvalidFilters(String query) throws Exception {
    mvc.perform(asCouncil(get(URL + "?" + query))).andExpect(status().isBadRequest());
    verifyNoInteractions(repository);
  }

  @Test
  void doesNotFindTransactionFromAnotherBethel() throws Exception {
    mvc.perform(asCouncil(get(URL + "/2"))).andExpect(status().isNotFound());
    mvc.perform(asCouncil(put(URL + "/2")).contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isNotFound());
    mvc.perform(asCouncil(delete(URL + "/2"))).andExpect(status().isNotFound());
    verify(repository, never()).save(any());
    verify(repository, never()).delete(any(FinancialTransaction.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"outsider", "member", "inactive", "missing"})
  void deniesEveryOperationWithoutActiveAdministrativeLink(String actor) throws Exception {
    Permission permission = new Permission();
    permission.setActive(!"inactive".equals(actor));
    permission.setRole("member".equals(actor) ? Role.MEMBRO_ATIVO : Role.CONSELHO_DE_BETHEL);
    Bethel bethel = new Bethel();
    bethel.setId("outsider".equals(actor) ? 20L : 10L);
    permission.setBethel(bethel);
    when(permissions.findByUserEmail("actor@test.com"))
        .thenReturn("missing".equals(actor) ? Optional.empty() : Optional.of(permission));
    for (MockHttpServletRequestBuilder request : List.of(get(URL), get(URL + "/1"), post(URL),
        put(URL + "/1"), delete(URL + "/1"))) {
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
    for (MockHttpServletRequestBuilder request : List.of(get(URL), get(URL + "/1"), post(URL),
        put(URL + "/1"), delete(URL + "/1"))) {
      mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(BODY))
          .andExpect(status().isUnauthorized());
    }
    verifyNoInteractions(repository);
  }
}
