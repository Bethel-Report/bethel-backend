
package com.bethelreport.backend.bethel.repositories;

import com.bethelreport.backend.bethel.entities.Bethel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BethelRepository extends JpaRepository<Bethel, Long> {
  Optional<Bethel> findByNumber(Integer number);

  boolean existsByNumber(Integer number);

  List<Bethel> findByNameContainingIgnoreCase(String name);

  boolean existsByNameIgnoreCase(String name);

  @Query("""
      SELECT b
      FROM Bethel b
      WHERE (:name IS NULL OR
          LOWER(b.name) LIKE LOWER(CONCAT('%', :name, '%')))
      AND (:active IS NULL OR b.active = :active)
      """)
  List<Bethel> findByFilters(@Param("name") String name, @Param("active") Boolean active);
}
