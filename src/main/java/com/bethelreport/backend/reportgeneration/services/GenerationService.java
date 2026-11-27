package com.bethelreport.backend.reportgeneration.services;

import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.reportgeneration.dtos.CreateGenerationRequest;
import com.bethelreport.backend.reportgeneration.dtos.GenerationPage;
import com.bethelreport.backend.reportgeneration.dtos.GenerationResponse;
import com.bethelreport.backend.reportgeneration.entities.GenerationFormat;
import com.bethelreport.backend.reportgeneration.entities.GenerationRequest;
import com.bethelreport.backend.reportgeneration.entities.GenerationStatus;
import com.bethelreport.backend.reportgeneration.mappers.GenerationMapper;
import com.bethelreport.backend.reportgeneration.message.dtos.GenerationMessage;
import com.bethelreport.backend.reportgeneration.message.publishers.GenerationPublisher;
import com.bethelreport.backend.reportgeneration.repositories.GenerationRequestRepository;
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
public class GenerationService {

  public record DownloadResult(byte[] content, GenerationFormat format) {}


  private final GenerationRequestRepository repository;
  private final BethelRepository bethels;
  private final UserRepository users;
  private final AuthorizationService authorization;
  private final GenerationMapper mapper;
  private final GenerationPublisher publisher;
  private final MinioStorage storage;

  public GenerationService(GenerationRequestRepository repository, BethelRepository bethels,
      UserRepository users, AuthorizationService authorization, GenerationMapper mapper,
      GenerationPublisher publisher, MinioStorage storage) {
    this.repository = repository;
    this.bethels = bethels;
    this.users = users;
    this.authorization = authorization;
    this.mapper = mapper;
    this.publisher = publisher;
    this.storage = storage;
  }

  @Transactional
  public GenerationResponse create(Long bethelId, @Valid CreateGenerationRequest request,
      Authentication authentication) {
    requireAccess(bethelId, authentication);
    validatePeriod(request);

    GenerationRequest entity = new GenerationRequest();
    entity.setBethel(bethels.findById(bethelId)
        .orElseThrow(() -> new EntityNotFoundException("Bethel não encontrado.")));

    if (!(authentication instanceof JwtAuthenticationToken jwt)) {
      throw new AccessDeniedException("Autenticação necessária.");
    }
    String email = jwt.getToken().getClaimAsString("email");
    entity.setRequester(users.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new AccessDeniedException("Usuário não encontrado.")));

    entity.setGenerationType(request.type());
    entity.setFormat(request.format() != null ? request.format() : GenerationFormat.PDF);
    entity.setStatus(GenerationStatus.PENDING);
    entity.setPeriodStart(request.periodStart());
    entity.setPeriodEnd(request.periodEnd());
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(LocalDateTime.now());

    GenerationRequest saved = repository.save(entity);

    publisher.publish(new GenerationMessage(saved.getId(), bethelId, saved.getGenerationType(),
        saved.getFormat(), saved.getPeriodStart(), saved.getPeriodEnd()));

    return mapper.toResponse(saved);
  }

  public GenerationResponse getStatus(Long bethelId, Long id, Authentication authentication) {
    requireAccess(bethelId, authentication);
    return mapper.toResponse(find(bethelId, id));
  }

  public GenerationPage list(Long bethelId, int page, int size, Authentication authentication) {
    requireAccess(bethelId, authentication);
    validatePagination(page, size);
    if (!bethels.existsById(bethelId)) {
      throw new EntityNotFoundException("Bethel não encontrado.");
    }
    return mapper.toPage(repository.findByBethelId(bethelId,
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
  }

  public DownloadResult download(Long bethelId, Long id, Authentication authentication) {
    requireAccess(bethelId, authentication);
    GenerationRequest entity = find(bethelId, id);
    if (entity.getStatus() != GenerationStatus.COMPLETED || entity.getStorageKey() == null) {
      throw new IllegalStateException("Relatório ainda não está disponível para download.");
    }
    return new DownloadResult(storage.download(entity.getStorageKey()), entity.getFormat());
  }

  @Transactional
  public void updateStatus(Long id, GenerationStatus status, String storageKey,
      String failureReason) {
    GenerationRequest entity = repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Solicitação de geração não encontrada."));
    entity.setStatus(status);
    entity.setStorageKey(storageKey);
    entity.setFailureReason(failureReason);
    entity.setUpdatedAt(LocalDateTime.now());
    repository.save(entity);
  }

  private GenerationRequest find(Long bethelId, Long id) {
    return repository.findByIdAndBethelId(id, bethelId)
        .orElseThrow(() -> new EntityNotFoundException("Solicitação de geração não encontrada."));
  }

  private void validatePeriod(CreateGenerationRequest request) {
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
