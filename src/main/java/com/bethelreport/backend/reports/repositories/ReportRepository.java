package com.bethelreport.backend.reports.repositories;

import com.bethelreport.backend.reports.entities.Report;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {
  List<Report> findAllByBethelIdOrderByCreatedAtDesc(Long bethelId);

  List<Report> findAllByOrderByCreatedAtDesc();

  Optional<Report> findByBethelIdAndFormId(Long bethelId, Long formId);

  @Query("""
      SELECT r FROM Report r
      LEFT JOIN FETCH r.answers a
      LEFT JOIN FETCH a.field
      JOIN FETCH r.bethel
      JOIN FETCH r.form
      WHERE r.id = :id
      """)
  Optional<Report> findByIdWithAnswers(@Param("id") Long id);
}
