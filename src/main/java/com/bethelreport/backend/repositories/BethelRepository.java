
package com.bethelreport.backend.repositories;
import com.bethelreport.backend.entities.Bethel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface BethelRepository extends JpaRepository<Bethel, Long> {
    Optional<Bethel> findByNumber(Integer number);
    boolean existsByNumber(Integer number);
    List<Bethel> findByNameContainingIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
