package com.bethelreport.backend.forms.mappers;

import com.bethelreport.backend.forms.dtos.CreateFormRequest;
import com.bethelreport.backend.forms.dtos.FormFieldResponse;
import com.bethelreport.backend.forms.dtos.FormResponse;
import com.bethelreport.backend.forms.dtos.UpdateFormRequest;
import com.bethelreport.backend.forms.entities.Form;
import com.bethelreport.backend.forms.entities.FormField;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FormMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "clonedFromId", ignore = true)
  @Mapping(target = "fields", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Form toEntity(CreateFormRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "clonedFromId", ignore = true)
  @Mapping(target = "fields", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(UpdateFormRequest request, @MappingTarget Form form);

  FormResponse toResponse(Form form);

  FormFieldResponse toFieldResponse(FormField field);

  List<FormResponse> toResponseList(List<Form> forms);
}
