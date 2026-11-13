package com.bethelreport.backend.transactions.services;

import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.transactions.dtos.TransactionPage;
import com.bethelreport.backend.transactions.dtos.TransactionRequest;
import com.bethelreport.backend.transactions.dtos.TransactionResponse;
import com.bethelreport.backend.transactions.entities.FinancialTransaction;
import com.bethelreport.backend.transactions.entities.TransactionType;
import com.bethelreport.backend.transactions.mappers.TransactionMapper;
import com.bethelreport.backend.transactions.repositories.TransactionFilters;
import com.bethelreport.backend.transactions.repositories.TransactionRepository;
import com.bethelreport.backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class TransactionService {
  private final TransactionRepository repository;
  private final BethelRepository bethels;
  private final UserRepository users;
  private final AuthorizationService authorization;
  private final TransactionMapper mapper;

  public TransactionService(TransactionRepository repository, BethelRepository bethels,
      UserRepository users, AuthorizationService authorization, TransactionMapper mapper) {
    this.repository = repository;
    this.bethels = bethels;
    this.users = users;
    this.authorization = authorization;
    this.mapper = mapper;
  }

  @Transactional
  public TransactionResponse create(Long bethelId, @Valid TransactionRequest request,
      Authentication authentication) {
    requireAccess(bethelId, authentication);
    FinancialTransaction entity = mapper.toEntity(request);
    entity.setBethel(bethels.findById(bethelId)
        .orElseThrow(() -> new EntityNotFoundException("Bethel não encontrado.")));
    if (!(authentication instanceof JwtAuthenticationToken jwt)) {
      throw new AccessDeniedException("Autenticação necessária.");
    }
    String email = jwt.getToken().getClaimAsString("email");
    entity.setAuthor(users.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new AccessDeniedException("Usuário não encontrado.")));
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(LocalDateTime.now());
    return mapper.toResponse(repository.save(entity));
  }

  public TransactionPage list(Long bethelId, TransactionType type, LocalDate startDate,
      LocalDate endDate, int page, int size, Authentication authentication) {
    requireAccess(bethelId, authentication);
    validateQuery(startDate, endDate, page, size);
    if (!bethels.existsById(bethelId)) {
      throw new EntityNotFoundException("Bethel não encontrado.");
    }
    return mapper
        .toPage(repository.findAll(TransactionFilters.by(bethelId, type, startDate, endDate),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate", "id"))));
  }

  public TransactionResponse get(Long bethelId, Long id, Authentication authentication) {
    requireAccess(bethelId, authentication);
    return mapper.toResponse(find(bethelId, id));
  }

  @Transactional
  public TransactionResponse update(Long bethelId, Long id, @Valid TransactionRequest request,
      Authentication authentication) {
    requireAccess(bethelId, authentication);
    FinancialTransaction entity = find(bethelId, id);
    mapper.updateEntity(request, entity);
    entity.setUpdatedAt(LocalDateTime.now());
    return mapper.toResponse(repository.save(entity));
  }

  @Transactional
  public void delete(Long bethelId, Long id, Authentication authentication) {
    requireAccess(bethelId, authentication);
    repository.delete(find(bethelId, id));
  }

  private FinancialTransaction find(Long bethelId, Long id) {
    return repository.findByIdAndBethelId(id, bethelId)
        .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada."));
  }

  private void validateQuery(LocalDate startDate, LocalDate endDate, int page, int size) {
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new IllegalArgumentException("Data inicial deve ser anterior ou igual à data final.");
    }
    if (page < 0 || size < 1 || size > 100) {
      throw new IllegalArgumentException(
          "Página deve ser maior ou igual a zero e tamanho entre 1 e 100.");
    }
  }

  private void requireAccess(Long bethelId, Authentication authentication) {
    if (!authorization.canManageBethel(bethelId, authentication)) {
      throw new AccessDeniedException("Sem permissão para gerenciar as finanças deste Bethel.");
    }
  }

}
