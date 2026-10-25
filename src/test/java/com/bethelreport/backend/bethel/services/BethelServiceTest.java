package com.bethelreport.backend.bethel.services;

import com.bethelreport.backend.bethel.dtos.BethelResponse;
import com.bethelreport.backend.bethel.dtos.ValidateBethelRequest;
import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.users.services.PermissionService;
import com.bethelreport.backend.security.AuthorizationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BethelServiceTest {
  private BethelRepository repository;
  private PermissionService permissionService;
  private AuthorizationService authorizationService;
  private BethelService service;

  @BeforeEach
  void setUp() {
    repository = mock(BethelRepository.class);
    permissionService = mock(PermissionService.class);
    authorizationService = mock(AuthorizationService.class);
    when(authorizationService.canAssignRole(any(), any(), any())).thenReturn(true);
    service = new BethelService(repository, permissionService, authorizationService);
  }

  private ValidateBethelRequest createRequest() {
    return new ValidateBethelRequest(12, "  Lírios do Vale  ", LocalDate.of(2020, 5, 10),
        "  Alexandria  ", "  Rua Principal  ", "  bethel@example.com  ", "  Loja União  ");
  }

  private ValidateBethelRequest updateRequest(int number) {
    return new ValidateBethelRequest(number, "  Nome atualizado  ", LocalDate.of(2021, 2, 3),
        "  Natal  ", "  Rua Nova  ", "  novo@example.com  ", "  Loja Nova  ");
  }

  private Bethel entity(long id, int number, boolean active) {
    Bethel b = new Bethel();
    b.setId(id);
    b.setNumber(number);
    b.setName("Lírios do Vale");
    b.setFoundationDate(LocalDate.of(2020, 5, 10));
    b.setCity("Alexandria");
    b.setAddress("Rua Principal");
    b.setEmail("bethel@example.com");
    b.setSponsoringLodge("Loja União");
    b.setActive(active);
    return b;
  }

  private void saveReturnsArgument() {
    when(repository.save(any(Bethel.class))).thenAnswer(inv -> {
      Bethel b = inv.getArgument(0);
      if (b.getId() == null)
        b.setId(1L);
      return b;
    });
  }

  private void assertStatus(HttpStatus status, Runnable action) {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, action::run);
    assertEquals(status, ex.getStatusCode());
  }

  @Test
  void createsBethelAndNormalizesText() {
    saveReturnsArgument();
    BethelResponse result = service.create(createRequest());
    assertEquals(1L, result.id());
    assertEquals(12, result.number());
    assertEquals("Lírios do Vale", result.name());
    assertEquals("Alexandria", result.city());
    assertEquals("Rua Principal", result.address());
    assertEquals("bethel@example.com", result.email());
    assertEquals("Loja União", result.sponsoringLodge());
    assertEquals(LocalDate.of(2020, 5, 10), result.foundationDate());
    assertTrue(result.active());
    verify(repository).existsByNumber(12);
    verify(repository).save(any(Bethel.class));
  }

  @Test
  void rejectsDuplicateNumberOnCreate() {
    when(repository.existsByNumber(12)).thenReturn(true);
    assertStatus(HttpStatus.CONFLICT, () -> service.create(createRequest()));
    verify(repository, never()).save(any());
  }

  @Test
  void listsAllWhenFilterIsNullOrBlank() {
    when(repository.findAll()).thenReturn(List.of(entity(1, 12, true), entity(2, 13, false)));
    assertEquals(2, service.findAll(null).size());
    assertEquals(2, service.findAll("  ").size());
    verify(repository, times(2)).findAll();
    verify(repository, never()).findByNameContainingIgnoreCase(any());
  }

  @Test
  void listsByTrimmedName() {
    when(repository.findByNameContainingIgnoreCase("Lírios"))
        .thenReturn(List.of(entity(1, 12, true)));
    assertEquals(1, service.findAll("  Lírios  ").size());
    verify(repository).findByNameContainingIgnoreCase("Lírios");
    verify(repository, never()).findAll();
  }

  @Test
  void returnsEmptyListWhenNoMatches() {
    when(repository.findByNameContainingIgnoreCase("inexistente")).thenReturn(List.of());
    assertTrue(service.findAll("inexistente").isEmpty());
  }

  @Test
  void findsBethelById() {
    when(repository.findById(1L)).thenReturn(Optional.of(entity(1, 12, true)));
    assertEquals(12, service.findById(1L).number());
  }

  @Test
  void rejectsMissingBethelOnFind() {
    assertStatus(HttpStatus.NOT_FOUND, () -> service.findById(999L));
  }

  @Test
  void updatesBethelAndNormalizesText() {
    Bethel original = entity(1, 12, false);
    when(repository.findById(1L)).thenReturn(Optional.of(original));
    saveReturnsArgument();
    BethelResponse result = service.update(1L, updateRequest(13));
    assertEquals(13, result.number());
    assertEquals("Nome atualizado", result.name());
    assertEquals("Natal", result.city());
    assertEquals("Rua Nova", result.address());
    assertEquals("novo@example.com", result.email());
    assertEquals("Loja Nova", result.sponsoringLodge());
    assertEquals(LocalDate.of(2021, 2, 3), result.foundationDate());
    assertFalse(result.active());
    verify(repository).existsByNumber(13);
    verify(repository).save(original);
  }

  @Test
  void updatesWithoutCheckingDuplicateWhenNumberUnchanged() {
    when(repository.findById(1L)).thenReturn(Optional.of(entity(1, 12, true)));
    saveReturnsArgument();
    assertEquals(12, service.update(1L, updateRequest(12)).number());
    verify(repository, never()).existsByNumber(any());
  }

  @Test
  void rejectsDuplicateNumberOnUpdate() {
    when(repository.findById(1L)).thenReturn(Optional.of(entity(1, 12, true)));
    when(repository.existsByNumber(13)).thenReturn(true);
    assertStatus(HttpStatus.CONFLICT, () -> service.update(1L, updateRequest(13)));
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsMissingBethelOnUpdate() {
    assertStatus(HttpStatus.NOT_FOUND, () -> service.update(999L, updateRequest(13)));
    verify(repository, never()).save(any());
  }

  @Test
  void activatesInactiveBethel() {
    Bethel original = entity(1, 12, false);
    when(repository.findById(1L)).thenReturn(Optional.of(original));
    saveReturnsArgument();
    assertTrue(service.activate(1L).active());
    verify(repository).save(original);
  }

  @Test
  void deactivatesActiveBethel() {
    Bethel original = entity(1, 12, true);
    when(repository.findById(1L)).thenReturn(Optional.of(original));
    saveReturnsArgument();
    assertFalse(service.deactivate(1L).active());
    verify(repository).save(original);
    verify(permissionService).deactivateByBethelId(1L);
  }

  @Test
  void rejectsMissingBethelOnActivate() {
    assertStatus(HttpStatus.NOT_FOUND, () -> service.activate(999L));
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsMissingBethelOnDeactivate() {
    assertStatus(HttpStatus.NOT_FOUND, () -> service.deactivate(999L));
    verify(repository, never()).save(any());
  }
}
