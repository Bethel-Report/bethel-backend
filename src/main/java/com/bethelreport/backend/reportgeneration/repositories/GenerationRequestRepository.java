package com.bethelreport.backend.reportgeneration.repositories;

import com.bethelreport.backend.reportgeneration.entities.GenerationRequest;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationRequestRepository extends JpaRepository<GenerationRequest, Long> {

  Page<GenerationRequest> findByBethelId(Long bethelId, Pageable pageable);

  Optional<GenerationRequest> findByIdAndBethelId(Long id, Long bethelId);
}
