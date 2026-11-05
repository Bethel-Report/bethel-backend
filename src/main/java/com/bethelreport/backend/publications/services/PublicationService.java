package com.bethelreport.backend.publications.services;

import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.publications.dtos.PublicationRequest;
import com.bethelreport.backend.publications.dtos.PublicationResponse;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationType;
import com.bethelreport.backend.publications.mappers.PublicationMapper;
import com.bethelreport.backend.publications.repositories.PublicationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PublicationService {
  private final PublicationRepository publicationRepository;
  private final PublicationRules rules;
  private final PublicationAccess access;
  private final PublicationMapper mapper;

  public PublicationService(PublicationRepository publicationRepository, PublicationRules rules,
      PublicationAccess access, PublicationMapper mapper) {
    this.publicationRepository = publicationRepository;
    this.rules = rules;
    this.access = access;
    this.mapper = mapper;
  }

  @Transactional
  public PublicationResponse create(PublicationRequest request, Authentication authentication) {
    Permission actor = access.actor(authentication);
    access.requireManagement(actor, request.bethelId());
    Publication publication = new Publication();
    publication.setAuthor(actor.getUser());
    publication.setCreatedAt(LocalDateTime.now());
    rules.apply(publication, request, actor);
    return mapper.toResponse(publicationRepository.save(publication));
  }

  public Page<PublicationResponse> list(int page, int size, Sort.Direction order, Long bethelFilter,
      PublicationType type, boolean management, Authentication authentication) {
    Permission actor = access.actor(authentication);
    if (page < 0 || size < 1 || size > 100) {
      throw new IllegalArgumentException("Página deve ser >= 0 e tamanho entre 1 e 100.");
    }
    Long bethelId = actor.getBethel() == null ? null : actor.getBethel().getId();
    if (management) {
      access.requireManagement(actor, bethelId);
    }
    return publicationRepository.findFeed(management, actor.getRole() == Role.GRANDE_CONSELHO,
        bethelId, actor.getUser().getId(), LocalDateTime.now(), bethelFilter, type,
        PageRequest.of(page, size, Sort.by(order, "timelineAt", "id"))).map(mapper::toResponse);
  }

  public PublicationResponse get(Long id, boolean management, Authentication authentication) {
    Permission actor = access.actor(authentication);
    Publication publication = find(id);
    if (management) {
      access.requireManagement(actor, access.bethelId(publication));
    } else {
      access.requireRead(actor, publication);
    }
    return mapper.toResponse(publication);
  }

  @Transactional
  public PublicationResponse update(Long id, PublicationRequest request,
      Authentication authentication) {
    Permission actor = access.actor(authentication);
    Publication publication = publicationRepository.findForUpdate(id)
        .orElseThrow(() -> new EntityNotFoundException("Publicação não encontrada."));
    access.requireManagement(actor, access.bethelId(publication));
    access.requireManagement(actor, request.bethelId());
    rules.apply(publication, request, actor);
    return mapper.toResponse(publicationRepository.save(publication));
  }

  public Publication find(Long id) {
    return publicationRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Publicação não encontrada."));
  }
}
