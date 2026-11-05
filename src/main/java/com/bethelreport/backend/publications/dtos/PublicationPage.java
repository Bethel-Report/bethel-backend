package com.bethelreport.backend.publications.dtos;

import java.util.List;
import org.springframework.data.domain.Page;

public record PublicationPage(List<PublicationResponse> content, int number, int size,
    long totalElements, int totalPages) {
  public static PublicationPage from(Page<PublicationResponse> page) {
    return new PublicationPage(page.getContent(), page.getNumber(), page.getSize(),
        page.getTotalElements(), page.getTotalPages());
  }
}
