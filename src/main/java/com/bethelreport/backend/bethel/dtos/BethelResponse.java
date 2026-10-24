package com.bethelreport.backend.bethel.dtos;

import com.bethelreport.backend.bethel.entities.Bethel;
import java.time.LocalDate;

public record BethelResponse(Long id, Integer number, String name, LocalDate foundationDate,
    String city, String address, String email, String sponsoringLodge, boolean active) {

  public static BethelResponse fromEntity(Bethel bethel) {
    return new BethelResponse(bethel.getId(), bethel.getNumber(), bethel.getName(),
        bethel.getFoundationDate(), bethel.getCity(), bethel.getAddress(), bethel.getEmail(),
        bethel.getSponsoringLodge(), bethel.isActive());
  }
}
