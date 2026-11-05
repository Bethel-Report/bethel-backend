package com.bethelreport.backend.publications.services;

import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationAudience;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class PublicationAccess {
  private final PermissionRepository permissionRepository;

  public PublicationAccess(PermissionRepository permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  public Permission actor(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwt)) {
      throw new AccessDeniedException("Autenticação necessária.");
    }
    String email = jwt.getToken().getClaimAsString("email");
    if (email == null || email.isBlank()) {
      throw new AccessDeniedException("Usuário não identificado.");
    }
    return permissionRepository.findByUserEmail(email)
        .filter(p -> p.isActive() && p.getUser() != null && p.getUser().isActive()
            && p.getRole() != null && (p.getBethel() == null || p.getBethel().isActive()))
        .orElseThrow(() -> new AccessDeniedException("Permissão ativa não encontrada."));
  }

  public void requireRead(Permission actor, Publication publication) {
    Long bethelId = actor.getBethel() == null ? null : actor.getBethel().getId();
    Long ownerBethelId = bethelId(publication);
    if (ownerBethelId != null && !ownerBethelId.equals(bethelId)) {
      throw new AccessDeniedException("Publicação local de outro Bethel.");
    }
    boolean targeted = publication.getAudience() == PublicationAudience.ALL_BETHELS
        || (bethelId != null && publication.getTargetBethelIds().contains(bethelId))
        || publication.getTargetUserIds().contains(actor.getUser().getId());
    if (!targeted || !withinDisplayPeriod(publication, LocalDateTime.now())) {
      throw new AccessDeniedException("Publicação indisponível para este usuário.");
    }
  }

  private boolean withinDisplayPeriod(Publication publication, LocalDateTime now) {
    boolean started = publication.getDisplayFrom() == null
        || !publication.getDisplayFrom().isAfter(now);
    boolean notExpired = publication.getDisplayUntil() == null
        || publication.getDisplayUntil().isAfter(now);
    return started && notExpired;
  }

  public void requireManagement(Permission actor, Long bethelId) {
    if (actor.getRole() == Role.GRANDE_CONSELHO) {
      return;
    }
    if (actor.getRole() != Role.CONSELHO_DE_BETHEL || bethelId == null || actor.getBethel() == null
        || !Objects.equals(bethelId, actor.getBethel().getId()) || !actor.getBethel().isActive()) {
      throw new AccessDeniedException("Sem permissão para gerenciar esta publicação.");
    }
  }

  public Long bethelId(Publication publication) {
    return publication.getBethel() == null ? null : publication.getBethel().getId();
  }
}
