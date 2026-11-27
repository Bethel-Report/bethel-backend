package com.bethelreport.backend.notifications.repositories;

import com.bethelreport.backend.notifications.entities.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  @Query(value = """
      SELECT DISTINCT p.user_id
      FROM publications pub
      CROSS JOIN permissions p
      JOIN users u ON u.id = p.user_id AND u.active = true
      LEFT JOIN bethels b ON b.id = p.bethel_id
      WHERE pub.id = :publicationId AND p.active = true
        AND (p.bethel_id IS NULL OR b.active = true)
        AND (
          pub.audience = 'ALL_BETHELS'
          OR (pub.audience = 'BETHELS' AND EXISTS (
            SELECT 1 FROM publication_target_bethels ptb
            WHERE ptb.publication_id = pub.id AND ptb.bethel_id = p.bethel_id))
          OR (pub.audience = 'USERS' AND EXISTS (
            SELECT 1 FROM publication_target_users ptu
            WHERE ptu.publication_id = pub.id AND ptu.user_id = p.user_id))
        )
      """, nativeQuery = true)
  List<Long> findPublicationRecipientIds(@Param("publicationId") Long publicationId);

  @Query(value = """
      SELECT DISTINCT p.user_id
      FROM permissions p
      JOIN users u ON u.id = p.user_id AND u.active = true
      LEFT JOIN bethels b ON b.id = p.bethel_id
      WHERE p.active = true AND (p.bethel_id IS NULL OR b.active = true)
      """, nativeQuery = true)
  List<Long> findActiveUserRecipientIds();

  @Query(value = """
      SELECT DISTINCT p.user_id
      FROM permissions p
      JOIN users u ON u.id = p.user_id AND u.active = true
      WHERE p.active = true AND p.role = 'GRANDE_CONSELHO'
      """, nativeQuery = true)
  List<Long> findGrandeConselhoRecipientIds();

  @Query(value = """
      SELECT DISTINCT p.user_id
      FROM permissions p
      JOIN users u ON u.id = p.user_id AND u.active = true
      JOIN bethels b ON b.id = p.bethel_id AND b.active = true
      WHERE p.active = true AND p.role = 'CONSELHO_DE_BETHEL' AND p.bethel_id = :bethelId
      """, nativeQuery = true)
  List<Long> findBethelCouncilRecipientIds(@Param("bethelId") Long bethelId);
}
