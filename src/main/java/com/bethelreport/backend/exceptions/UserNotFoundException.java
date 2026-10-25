package com.bethelreport.backend.exceptions;

public class UserNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public UserNotFoundException(Long id) {
    super("Usuário não encontrado com id: " + id);
  }
}
