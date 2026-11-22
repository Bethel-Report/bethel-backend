package com.bethelreport.backend.reports.services;

import com.bethelreport.backend.forms.entities.Form;
import com.bethelreport.backend.forms.entities.FormField;
import com.bethelreport.backend.forms.entities.FormStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

final class ReportValidation {
  private static final String FIELD_PREFIX = "Campo '";

  private ReportValidation() {
  }

  static void requireFormOpen(Form form) {
    if (form.getStatus() == FormStatus.CLOSED) {
      throw new IllegalStateException("O formulário foi encerrado.");
    }
    LocalDate today = LocalDate.now();
    if (form.getOpeningDate() != null && today.isBefore(form.getOpeningDate())) {
      throw new IllegalStateException(
          "O período de preenchimento do formulário ainda não iniciou.");
    }
    if (form.getClosingDate() != null && today.isAfter(form.getClosingDate())) {
      throw new IllegalStateException("O período de preenchimento do formulário já encerrou.");
    }
  }

  static void validateAnswerValue(FormField field, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    switch (field.getType()) {
      case NUMBER -> validateNumber(field, value);
      case DATE -> validateDate(field, value);
      case BOOLEAN -> validateBoolean(field, value);
      case SELECT -> validateSelect(field, value);
      default -> {
      }
    }
  }

  private static void validateNumber(FormField field, String value) {
    try {
      new BigDecimal(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          FIELD_PREFIX + field.getLabel() + "' deve conter um valor numérico.", e);
    }
  }

  private static void validateDate(FormField field, String value) {
    try {
      LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          FIELD_PREFIX + field.getLabel() + "' deve conter uma data válida (yyyy-MM-dd).", e);
    }
  }

  private static void validateBoolean(FormField field, String value) {
    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
      throw new IllegalArgumentException(
          FIELD_PREFIX + field.getLabel() + "' deve conter verdadeiro ou falso.");
    }
  }

  private static void validateSelect(FormField field, String value) {
    if (!field.getOptions().contains(value)) {
      throw new IllegalArgumentException(
          FIELD_PREFIX + field.getLabel() + "' deve conter uma das opções disponíveis.");
    }
  }
}
