package com.bethelreport.backend.permissions.repositories;

import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

  Optional<Permission> findByUserId(Long userId);

  List<Permission> findAllByBethelId(Long bethelId);

  List<Permission> findAllByBethelIdAndRoleAndActiveTrueAndUserActiveTrueOrderByIdAsc(Long bethelId,
      Role role);

  boolean existsByUserId(Long userId);

  Optional<Permission> findByUserEmail(String email);
}
