package com.bethelreport.backend.users.repositories;

import com.bethelreport.backend.users.entities.Permission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

  Optional<Permission> findByUserId(Long userId);

  boolean existsByUserId(Long userId);
}
