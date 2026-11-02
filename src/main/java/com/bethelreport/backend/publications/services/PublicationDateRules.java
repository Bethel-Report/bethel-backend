package com.bethelreport.backend.publications.services;

import com.bethelreport.backend.publications.dtos.PublicationRequest;
import com.bethelreport.backend.publications.entities.PublicationType;
import org.springframework.stereotype.Component;

@Component
public class PublicationDateRules {
  public void validate(PublicationRequest request) {
    if (request.type() == PublicationType.ACTIVITY) {
      requireActivityDate(request);
    } else if (request.activityDate() != null) {
      throw new IllegalArgumentException("Data de realização é exclusiva de atividades.");
    }
    if (request.type() == PublicationType.ANNOUNCEMENT) {
      requireAnnouncementPeriod(request);
    }
    if (request.displayUntil() != null) {
      validatePeriod(request);
    }
  }

  private void requireActivityDate(PublicationRequest request) {
    if (request.activityDate() == null || request.bethelId() == null) {
      throw new IllegalArgumentException("Atividade exige data de realização e Bethel.");
    }
  }

  private void requireAnnouncementPeriod(PublicationRequest request) {
    if (request.displayFrom() == null || request.displayUntil() == null) {
      throw new IllegalArgumentException("Comunicado exige início e fim da exibição.");
    }
  }

  private void validatePeriod(PublicationRequest request) {
    if (request.displayFrom() == null || !request.displayUntil().isAfter(request.displayFrom())) {
      throw new IllegalArgumentException("Fim da exibição deve ser posterior ao início.");
    }
  }
}
