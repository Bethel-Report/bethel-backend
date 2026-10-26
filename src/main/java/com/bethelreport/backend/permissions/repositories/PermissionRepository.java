package com.bethelreport.backend.permissions.repositories;

import com.bethelreport.backend.permissions.entities.Permission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

  Optional<Permission> findByUserId(Long userId);

  java.util.List<Permission> findAllByBethelId(Long bethelId);

  boolean existsByUserId(Long userId);

  Optional<Permission> findByUserEmail(String email);
}
