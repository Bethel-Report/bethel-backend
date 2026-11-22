package com.bethelreport.backend.reports.mappers;

import com.bethelreport.backend.reports.dtos.ReportAnswerResponse;
import com.bethelreport.backend.reports.dtos.ReportResponse;
import com.bethelreport.backend.reports.entities.Report;
import com.bethelreport.backend.reports.entities.ReportAnswer;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReportMapper {
  @Mapping(source = "bethel.id", target = "bethelId")
  @Mapping(source = "bethel.name", target = "bethelName")
  @Mapping(source = "form.id", target = "formId")
  @Mapping(source = "form.name", target = "formName")
  ReportResponse toResponse(Report report);

  @Mapping(source = "field.id", target = "fieldId")
  @Mapping(source = "field.label", target = "fieldLabel")
  @Mapping(source = "field.type", target = "fieldType")
  ReportAnswerResponse toAnswerResponse(ReportAnswer answer);

  List<ReportResponse> toResponseList(List<Report> reports);
}
