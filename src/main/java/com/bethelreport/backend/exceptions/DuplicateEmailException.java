package com.bethelreport.backend.exceptions;

public class DuplicateEmailException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DuplicateEmailException(String email) {
    super("E-mail já cadastrado: " + email);
  }
}
