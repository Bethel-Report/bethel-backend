package com.bethelreport.backend.bethel.dtos;

import java.time.LocalDate;

public record BethelResponse(Long id, Integer number, String name, LocalDate foundationDate,
    String city, String address, String email, String sponsoringLodge, boolean active) {
}
