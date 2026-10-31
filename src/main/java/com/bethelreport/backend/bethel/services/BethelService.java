
package com.bethelreport.backend.bethel.services;

import com.bethelreport.backend.bethel.dtos.BethelResponse;
import com.bethelreport.backend.bethel.dtos.ValidateBethelRequest;
import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.mappers.BethelMapper;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.permissions.services.PermissionService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BethelService {

  private final BethelRepository bethelRepository;
  private final PermissionService permissionService;
  private final AuthorizationService authorizationService;
  private final BethelMapper bethelMapper;

  public BethelService(BethelRepository bethelRepository, PermissionService permissionService,
      AuthorizationService authorizationService, BethelMapper bethelMapper) {
    this.bethelRepository = bethelRepository;
    this.permissionService = permissionService;
    this.authorizationService = authorizationService;
    this.bethelMapper = bethelMapper;
  }

  @Transactional
  public BethelResponse create(ValidateBethelRequest request) {
    requireCanAssignRole(Role.GRANDE_CONSELHO, null);

    if (bethelRepository.existsByNumber(request.number())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Já existe um Bethel cadastrado com este número.");
    }

    Bethel bethel = bethelMapper.toEntity(request);

    return bethelMapper.toResponse(bethelRepository.save(bethel));
  }

  @Transactional(readOnly = true)
  public List<BethelResponse> findAll(String name, Boolean active) {
    String normalizedName = name == null || name.isBlank() ? null : name.trim();
    return bethelMapper.toList(bethelRepository.findByFilters(normalizedName, active));
  }

  @Transactional(readOnly = true)
  public BethelResponse findById(Long id) {
    return bethelMapper.toResponse(getBethelOrThrow(id));
  }

  @Transactional
  public BethelResponse update(Long id, ValidateBethelRequest request) {
    requireCanAssignRole(Role.CONSELHO_DE_BETHEL, id);

    Bethel bethel = getBethelOrThrow(id);

    if (!bethel.getNumber().equals(request.number())
        && bethelRepository.existsByNumber(request.number())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Já existe um Bethel cadastrado com este número.");
    }

    bethelMapper.updateEntity(request, bethel);
    return bethelMapper.toResponse(bethelRepository.save(bethel));
  }

  @Transactional
  public BethelResponse activate(Long id) {
    requireGrandeConselho();

    Bethel bethel = getBethelOrThrow(id);
    bethel.setActive(true);

    return bethelMapper.toResponse(bethelRepository.save(bethel));
  }

  @Transactional
  public BethelResponse deactivate(Long id) {
    requireGrandeConselho();

    Bethel bethel = getBethelOrThrow(id);
    bethel.setActive(false);
    permissionService.deactivateByBethelId(id);

    return bethelMapper.toResponse(bethelRepository.save(bethel));
  }

  private Bethel getBethelOrThrow(Long id) {

    return bethelRepository.findById(id).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bethel não encontrado."));
  }

  private void requireCanAssignRole(Role role, Long bethelId) {
    if (!authorizationService.canAssignRole(SecurityContextHolder.getContext().getAuthentication(),
        role, bethelId)) {
      throw new AccessDeniedException("Você não tem permissão para gerenciar este Bethel");
    }
  }

  private void requireGrandeConselho() {
    if (!authorizationService
        .isGrandeConselho(SecurityContextHolder.getContext().getAuthentication())) {
      throw new AccessDeniedException("Apenas o Grande Conselho pode gerenciar este Bethel");
    }
  }
}
