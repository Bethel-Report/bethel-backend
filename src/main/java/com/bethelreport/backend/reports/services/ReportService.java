package com.bethelreport.backend.reports.services;

import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.reports.dtos.CreateReportRequest;
import com.bethelreport.backend.reports.dtos.ReportPage;
import com.bethelreport.backend.reports.dtos.ReportResponse;
import com.bethelreport.backend.reports.entities.ReportRequest;
import com.bethelreport.backend.reports.entities.ReportStatus;
import com.bethelreport.backend.reports.mappers.ReportMapper;
import com.bethelreport.backend.reports.message.dtos.ReportMessage;
import com.bethelreport.backend.reports.message.publishers.ReportPublisher;
import com.bethelreport.backend.reports.repositories.ReportRequestRepository;
import com.bethelreport.backend.storage.MinioStorage;
import com.bethelreport.backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
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
public class ReportService {

  private final ReportRequestRepository repository;
  private final BethelRepository bethels;
  private final UserRepository users;
  private final AuthorizationService authorization;
  private final ReportMapper mapper;
  private final ReportPublisher publisher;
  private final MinioStorage storage;

  public ReportService(ReportRequestRepository repository, BethelRepository bethels,
      UserRepository users, AuthorizationService authorization, ReportMapper mapper,
      ReportPublisher publisher, MinioStorage storage) {
    this.repository = repository;
    this.bethels = bethels;
    this.users = users;
    this.authorization = authorization;
    this.mapper = mapper;
    this.publisher = publisher;
    this.storage = storage;
  }

  @Transactional
  public ReportResponse create(Long bethelId, @Valid CreateReportRequest request,
      Authentication authentication) {
    requireAccess(bethelId, authentication);
    validatePeriod(request);

    ReportRequest entity = new ReportRequest();
    entity.setBethel(bethels.findById(bethelId)
        .orElseThrow(() -> new EntityNotFoundException("Bethel não encontrado.")));

    if (!(authentication instanceof JwtAuthenticationToken jwt)) {
      throw new AccessDeniedException("Autenticação necessária.");
    }
    String email = jwt.getToken().getClaimAsString("email");
    entity.setRequester(users.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new AccessDeniedException("Usuário não encontrado.")));

    entity.setType(request.type());
    entity.setStatus(ReportStatus.PENDING);
    entity.setPeriodStart(request.periodStart());
    entity.setPeriodEnd(request.periodEnd());
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(LocalDateTime.now());

    ReportRequest saved = repository.save(entity);

    publisher.publish(new ReportMessage(saved.getId(), bethelId, saved.getType(),
        saved.getPeriodStart(), saved.getPeriodEnd()));

    return mapper.toResponse(saved);
  }

  public ReportResponse getStatus(Long bethelId, Long id, Authentication authentication) {
    requireAccess(bethelId, authentication);
    return mapper.toResponse(find(bethelId, id));
  }

  public ReportPage list(Long bethelId, int page, int size, Authentication authentication) {
    requireAccess(bethelId, authentication);
    validatePagination(page, size);
    if (!bethels.existsById(bethelId)) {
      throw new EntityNotFoundException("Bethel não encontrado.");
    }
    return mapper.toPage(repository.findByBethelId(bethelId,
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
  }

  public byte[] download(Long bethelId, Long id, Authentication authentication) {
    requireAccess(bethelId, authentication);
    ReportRequest entity = find(bethelId, id);
    if (entity.getStatus() != ReportStatus.COMPLETED || entity.getStorageKey() == null) {
      throw new IllegalStateException("Relatório ainda não está disponível para download.");
    }
    return storage.download(entity.getStorageKey());
  }

  @Transactional
  public void updateStatus(Long id, ReportStatus status, String storageKey, String failureReason) {
    ReportRequest entity = repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Solicitação de relatório não encontrada."));
    entity.setStatus(status);
    entity.setStorageKey(storageKey);
    entity.setFailureReason(failureReason);
    entity.setUpdatedAt(LocalDateTime.now());
    repository.save(entity);
  }

  private ReportRequest find(Long bethelId, Long id) {
    return repository.findByIdAndBethelId(id, bethelId)
        .orElseThrow(() -> new EntityNotFoundException("Solicitação de relatório não encontrada."));
  }

  private void validatePeriod(CreateReportRequest request) {
    if (request.periodStart().isAfter(request.periodEnd())) {
      throw new IllegalArgumentException("Data inicial deve ser anterior ou igual à data final.");
    }
  }

  private void validatePagination(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new IllegalArgumentException(
          "Página deve ser maior ou igual a zero e tamanho entre 1 e 100.");
    }
  }

  private void requireAccess(Long bethelId, Authentication authentication) {
    if (!authorization.canManageBethel(bethelId, authentication)) {
      throw new AccessDeniedException("Sem permissão para gerenciar relatórios deste Bethel.");
    }
  }
}
