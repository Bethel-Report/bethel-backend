package com.bethelreport.backend.reports.repositories;

import com.bethelreport.backend.reports.entities.ReportRequest;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRequestRepository extends JpaRepository<ReportRequest, Long> {

  Page<ReportRequest> findByBethelId(Long bethelId, Pageable pageable);

  Optional<ReportRequest> findByIdAndBethelId(Long id, Long bethelId);
}
