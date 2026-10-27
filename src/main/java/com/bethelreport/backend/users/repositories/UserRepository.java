package com.bethelreport.backend.users.repositories;

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
      SELECT p.user
      FROM Permission p
      WHERE p.bethel.id = :bethelId
        AND p.active = true
      """)
  List<User> findAllByBethelId(@Param("bethelId") Long bethelId);
}
