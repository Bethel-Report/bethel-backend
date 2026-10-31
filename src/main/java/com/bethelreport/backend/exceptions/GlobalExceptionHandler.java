package com.bethelreport.backend.exceptions;

import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
    return buildSecurityError(HttpStatus.UNAUTHORIZED, "Unauthorized", "Autenticação necessária.");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
    return buildSecurityError(HttpStatus.FORBIDDEN, "Forbidden", "Acesso não permitido.");
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
    return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidArgument(IllegalArgumentException ex) {
    return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidState(IllegalStateException ex) {
    return buildError(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataConflict(
      DataIntegrityViolationException ex) {
    return buildError(HttpStatus.CONFLICT, "Vínculo duplicado ou conflitante");
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
    return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(InvitationNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleInvitationNotFound(
      InvitationNotFoundException ex) {
    return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex) {
    return buildError(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
    return buildError(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(FieldError::getField, e -> {
          String msg = e.getDefaultMessage();
          return msg != null ? msg : "";
        }, (a, b) -> a));
    return ResponseEntity.badRequest().body(Map.of("status", HttpStatus.BAD_REQUEST.value(),
        "message", "Dados inválidos", "errors", fieldErrors));
  }

  private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message));
  }

  private ResponseEntity<Map<String, Object>> buildSecurityError(HttpStatus status, String error,
      String message) {
    return ResponseEntity.status(status)
        .body(Map.of("status", status.value(), "error", error, "message", message));
  }
}
