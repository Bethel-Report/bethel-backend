package com.bethelreport.backend.forms.repositories;

import com.bethelreport.backend.forms.entities.Form;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormRepository extends JpaRepository<Form, Long> {
  List<Form> findAllByOrderByReferenceYearDescIdDesc();
}
