package com.bethelreport.backend.forms.dtos;

import com.bethelreport.backend.forms.entities.FormFieldType;
import java.util.List;

public record FormFieldResponse(Long id, String label, FormFieldType type, boolean required,
    Integer displayOrder, List<String> options) {
}
