
package com.bethelreport.backend.bethel.repositories;

import com.bethelreport.backend.bethel.entities.Bethel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BethelRepository extends JpaRepository<Bethel, Long> {
  Optional<Bethel> findByNumber(Integer number);

  boolean existsByNumber(Integer number);

  List<Bethel> findByNameContainingIgnoreCase(String name);

  boolean existsByNameIgnoreCase(String name);
}
