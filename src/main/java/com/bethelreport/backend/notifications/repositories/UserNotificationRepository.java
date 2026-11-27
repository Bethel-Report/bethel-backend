package com.bethelreport.backend.notifications.repositories;

import com.bethelreport.backend.notifications.entities.UserNotification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
  @Query(value = """
      SELECT un FROM UserNotification un JOIN FETCH un.notification n
      WHERE un.user.email = :email
      ORDER BY n.createdAt DESC, un.id DESC
      """, countQuery = "SELECT COUNT(un) FROM UserNotification un WHERE un.user.email = :email")
  Page<UserNotification> findPageByUserEmail(@Param("email") String email, Pageable pageable);

  @Query("""
      SELECT un FROM UserNotification un JOIN FETCH un.notification n
      WHERE un.id = :id AND un.user.email = :email
      """)
  Optional<UserNotification> findOwnedById(@Param("id") Long id, @Param("email") String email);

  @Query("SELECT COUNT(un) FROM UserNotification un "
      + "WHERE un.user.email = :email AND un.seen = false")
  long countUnreadByUserEmail(@Param("email") String email);
}
