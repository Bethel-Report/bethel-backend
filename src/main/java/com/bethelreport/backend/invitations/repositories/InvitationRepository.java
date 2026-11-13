package com.bethelreport.backend.invitations.repositories;

import com.bethelreport.backend.invitations.entities.Invitation;
import com.bethelreport.backend.invitations.entities.InvitationStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
  Optional<Invitation> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndStatus(String email, InvitationStatus status);

  Optional<Invitation> findByEmailIgnoreCaseAndStatus(String email, InvitationStatus status);

  List<Invitation> findByBethelId(Long bethelId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT i
      FROM Invitation i
      WHERE LOWER(i.email) = LOWER(:email)
      """)
  Optional<Invitation> findByEmailForUpdate(String email);
}
