package com.bethelreport.backend.audit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Initializes transaction-local PostgreSQL audit context before entity writes. */
@Component
@RequiredArgsConstructor
public class AuditContextEntityListener {

  private final JdbcTemplate jdbcTemplate;

  @PrePersist
  public void beforePersist(Object entity) {
    setAuditContext(entity);
  }

  @PreUpdate
  public void beforeUpdate(Object entity) {
    setAuditContext(entity);
  }

  @PreRemove
  public void beforeRemove(Object entity) {
    setAuditContext(entity);
  }

  private void setAuditContext(Object entity) {
    if (entity == null) {
      throw new IllegalArgumentException("Audited entity must not be null");
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication instanceof JwtAuthenticationToken jwtAuthentication
        ? jwtAuthentication.getToken().getClaimAsString("email")
        : null;
    boolean systemOperation = email == null || email.isBlank();

    jdbcTemplate.queryForObject(
        "SELECT set_config('app.audit_user_id', COALESCE((SELECT u.id::text FROM users u "
            + "WHERE lower(u.email) = lower(?) LIMIT 1), ''), true), "
            + "set_config('app.audit_user_email', ?, true), "
            + "set_config('app.audit_is_system', ?, true)",
        (resultSet, rowNumber) -> Boolean.TRUE, systemOperation ? "" : email,
        systemOperation ? "" : email, Boolean.toString(systemOperation));
  }
}
