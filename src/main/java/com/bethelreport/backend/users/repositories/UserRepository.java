package com.bethelreport.backend.users.repositories;

import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.users.entities.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  @Query("""
      SELECT u
      FROM User u
      LEFT JOIN Permission p ON p.user.id = u.id
      WHERE (:email IS NULL OR LOWER(u.email) LIKE CONCAT('%', LOWER(CAST(:email AS string)), '%'))
        AND (:bethelId IS NULL OR p.bethel.id = :bethelId)
        AND (:status IS NULL OR u.active = :status)
        AND (:profile IS NULL OR p.role = :profile)
      """)
  List<User> searchUsers(@Param("email") String email, @Param("bethelId") Long bethelId,
      @Param("status") Boolean status, @Param("profile") Role profile);
}
