package com.bethelreport.backend.publications.repositories;

import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicationRepository extends JpaRepository<Publication, Long> {
  @Query("""
      SELECT p FROM Publication p
      WHERE (:type IS NULL OR p.type = :type)
        AND (:bethelFilter IS NULL OR p.bethel.id = :bethelFilter
        OR :bethelFilter MEMBER OF p.targetBethelIds
        OR p.audience = ALL_BETHELS
        OR (p.audience = USERS
            AND :bethelFilter = :bethelId))
        AND ((:management = true AND (:global = true OR p.bethel.id = :bethelId))
        OR (:management = false
          AND (p.bethel IS NULL OR p.bethel.id = :bethelId)
          AND (p.displayFrom IS NULL OR p.displayFrom <= :now)
          AND (p.displayUntil IS NULL OR p.displayUntil > :now)
          AND ((p.audience = ALL_BETHELS)
            OR (:bethelId MEMBER OF p.targetBethelIds)
            OR (:userId MEMBER OF p.targetUserIds))))
      """)
  Page<Publication> findFeed(@Param("management") boolean management,
      @Param("global") boolean global, @Param("bethelId") Long bethelId,
      @Param("userId") Long userId, @Param("now") LocalDateTime now,
      @Param("bethelFilter") Long bethelFilter, @Param("type") PublicationType type,
      Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Publication p WHERE p.id = :id")
  Optional<Publication> findForUpdate(@Param("id") Long id);
}
