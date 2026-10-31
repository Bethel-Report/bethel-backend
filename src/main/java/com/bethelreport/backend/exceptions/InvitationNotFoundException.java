package com.bethelreport.backend.exceptions;

public class InvitationNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InvitationNotFoundException(Long id) {
    super("Convite não encontrado com id: " + id);
  }
}
