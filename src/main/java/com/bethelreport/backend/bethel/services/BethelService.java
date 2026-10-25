
package com.bethelreport.backend.bethel.services;

import com.bethelreport.backend.bethel.dtos.BethelResponse;
import com.bethelreport.backend.bethel.dtos.ValidateBethelRequest;
import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.users.services.PermissionService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BethelService {

  private final BethelRepository bethelRepository;
  private final PermissionService permissionService;

  public BethelService(BethelRepository bethelRepository, PermissionService permissionService) {
    this.bethelRepository = bethelRepository;
    this.permissionService = permissionService;
  }

  @Transactional
  public BethelResponse create(ValidateBethelRequest request) {

    if (bethelRepository.existsByNumber(request.number())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Já existe um Bethel cadastrado com este número.");
    }

    Bethel bethel = new Bethel();

    updateFields(bethel, request.number(), request.name(), request.foundationDate(), request.city(),
        request.address(), request.email(), request.sponsoringLodge());

    bethel.setActive(true);

    return BethelResponse.fromEntity(bethelRepository.save(bethel));
  }

  @Transactional(readOnly = true)
  public List<BethelResponse> findAll(String name) {

    List<Bethel> bethels;

    if (name != null && !name.isBlank()) {
      bethels = bethelRepository.findByNameContainingIgnoreCase(name.trim());
    } else {
      bethels = bethelRepository.findAll();
    }

    return bethels.stream().map(BethelResponse::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public BethelResponse findById(Long id) {
    return BethelResponse.fromEntity(getBethelOrThrow(id));
  }

  @Transactional
  public BethelResponse update(Long id, ValidateBethelRequest request) {

    Bethel bethel = getBethelOrThrow(id);

    if (!bethel.getNumber().equals(request.number())
        && bethelRepository.existsByNumber(request.number())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Já existe um Bethel cadastrado com este número.");
    }

    updateFields(bethel, request.number(), request.name(), request.foundationDate(), request.city(),
        request.address(), request.email(), request.sponsoringLodge());

    return BethelResponse.fromEntity(bethelRepository.save(bethel));
  }

  @Transactional
  public BethelResponse activate(Long id) {

    Bethel bethel = getBethelOrThrow(id);
    bethel.setActive(true);

    return BethelResponse.fromEntity(bethelRepository.save(bethel));
  }

  @Transactional
  public BethelResponse deactivate(Long id) {

    Bethel bethel = getBethelOrThrow(id);
    bethel.setActive(false);
    permissionService.deactivateByBethelId(id);

    return BethelResponse.fromEntity(bethelRepository.save(bethel));
  }

  private Bethel getBethelOrThrow(Long id) {

    return bethelRepository.findById(id).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bethel não encontrado."));
  }

  private void updateFields(Bethel bethel, Integer number, String name, LocalDate foundationDate,
      String city, String address, String email, String sponsoringLodge) {

    bethel.setNumber(number);
    bethel.setName(name.trim());
    bethel.setFoundationDate(foundationDate);
    bethel.setCity(city.trim());
    bethel.setAddress(address.trim());
    bethel.setEmail(email.trim());
    bethel.setSponsoringLodge(sponsoringLodge.trim());
  }
}
