package com.bethelreport.backend.forms.dtos;

import com.bethelreport.backend.forms.entities.FormFieldType;
import java.util.List;

public record FormFieldResponse(Long id, String label, FormFieldType type, boolean required,
    String section, String key, boolean financial, String validationRules, Integer displayOrder,
    List<String> options) {
}
