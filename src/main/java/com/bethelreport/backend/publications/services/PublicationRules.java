package com.bethelreport.backend.publications.services;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.publications.dtos.PublicationRequest;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationAudience;
import com.bethelreport.backend.publications.entities.PublicationType;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class PublicationRules {
  private final BethelRepository bethels;
  private final PermissionRepository permissions;
  private final PublicationDateRules dateRules;

  public PublicationRules(BethelRepository bethels, PermissionRepository permissions,
      PublicationDateRules dateRules) {
    this.bethels = bethels;
    this.permissions = permissions;
    this.dateRules = dateRules;
  }

  public void apply(Publication publication, PublicationRequest request, Permission actor) {
    dateRules.validate(request);
    Set<Long> targetBethels = request.targetBethelIds() == null
        ? Set.of()
        : request.targetBethelIds();
    Set<Long> targetUsers = request.targetUserIds() == null ? Set.of() : request.targetUserIds();
    validateAudience(request, targetBethels, targetUsers);
    validateCouncilTargets(actor, request, targetBethels);
    final Bethel owner = request.bethelId() == null ? null : activeBethel(request.bethelId());
    targetBethels.forEach(this::activeBethel);
    targetUsers.forEach(userId -> validateTargetUser(userId, request.bethelId()));
    publication.setTitle(request.title().strip());
    publication.setContent(request.content().strip());
    publication.setType(request.type());
    publication.setActivityDate(request.activityDate());
    publication.setBethel(owner);
    publication.setAudience(request.audience());
    publication.setTargetBethelIds(new HashSet<>(targetBethels));
    publication.setTargetUserIds(new HashSet<>(targetUsers));
    publication.setDisplayFrom(request.displayFrom());
    publication.setDisplayUntil(request.displayUntil());
    publication.setTimelineAt(request.activityDate() == null
        ? publication.getCreatedAt()
        : request.activityDate().atStartOfDay());
    publication.setUpdatedAt(LocalDateTime.now());
  }

  private void validateCouncilTargets(Permission actor, PublicationRequest request,
      Set<Long> targetBethels) {
    if (actor.getRole() != Role.GRANDE_CONSELHO
        && (request.audience() == PublicationAudience.ALL_BETHELS
            || targetBethels.stream().anyMatch(id -> !id.equals(request.bethelId())))) {
      throw new AccessDeniedException("Público-alvo fora do próprio Bethel.");
    }
  }

  private void validateTargetUser(Long userId, Long bethelId) {
    Permission target = permissions.findByUserId(userId)
        .filter(p -> p.isActive() && p.getUser().isActive())
        .orElseThrow(() -> new IllegalArgumentException("Destinatário sem permissão ativa."));
    if (bethelId != null
        && (target.getBethel() == null || !bethelId.equals(target.getBethel().getId()))) {
      throw new AccessDeniedException("Destinatário de outro Bethel.");
    }
  }

  private void validateAudience(PublicationRequest request, Set<Long> targetBethels,
      Set<Long> targetUsers) {
    boolean valid = switch (request.audience()) {
      case ALL_BETHELS -> validStatewideTargets(request, targetBethels, targetUsers);
      case BETHELS -> !targetBethels.isEmpty() && targetUsers.isEmpty();
      case USERS -> targetBethels.isEmpty() && !targetUsers.isEmpty();
    };
    if (!valid) {
      throw new IllegalArgumentException("Público-alvo e destinatários incompatíveis.");
    }
    validateLocalAudience(request, targetBethels);
    validateActivityAudience(request, targetBethels);
  }

  private boolean validStatewideTargets(PublicationRequest request, Set<Long> bethels,
      Set<Long> users) {
    return bethels.isEmpty() && users.isEmpty() && request.bethelId() == null;
  }

  private void validateLocalAudience(PublicationRequest request, Set<Long> targetBethels) {
    if (request.bethelId() != null && request.audience() == PublicationAudience.BETHELS
        && !targetBethels.equals(Set.of(request.bethelId()))) {
      throw new IllegalArgumentException("Publicação local deve atingir apenas o próprio Bethel.");
    }
  }

  private void validateActivityAudience(PublicationRequest request, Set<Long> targetBethels) {
    if (request.type() == PublicationType.ACTIVITY
        && (request.audience() != PublicationAudience.BETHELS
            || !targetBethels.equals(Set.of(request.bethelId())))) {
      throw new IllegalArgumentException("Atividade deve ser visível ao Bethel da realização.");
    }
  }

  private Bethel activeBethel(Long id) {
    Bethel bethel = bethels.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Bethel não encontrado."));
    if (!bethel.isActive()) {
      throw new IllegalArgumentException("Bethel inativo.");
    }
    return bethel;
  }
}
