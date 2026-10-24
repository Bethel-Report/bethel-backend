
package com.bethelreport.backend.dtos.bethel;

import java.time.LocalDate;

import com.bethelreport.backend.entities.Bethel;

public record BethelResponse(Long id, Integer number, String name, LocalDate foundationDate,
    String city, String address, String email, String sponsoringLodge, boolean active) {

  public static BethelResponse fromEntity(Bethel bethel) {
    return new BethelResponse(bethel.getId(), bethel.getNumber(), bethel.getName(),
        bethel.getFoundationDate(), bethel.getCity(), bethel.getAddress(), bethel.getEmail(),
        bethel.getSponsoringLodge(), bethel.isActive());
  }
}
