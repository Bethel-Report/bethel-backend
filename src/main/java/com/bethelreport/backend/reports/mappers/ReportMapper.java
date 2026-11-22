package com.bethelreport.backend.reports.mappers;

import com.bethelreport.backend.reports.dtos.ReportPage;
import com.bethelreport.backend.reports.dtos.ReportResponse;
import com.bethelreport.backend.reports.entities.ReportRequest;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReportMapper {

  @Mapping(target = "bethelId", source = "bethel.id")
  @Mapping(target = "requesterId", source = "requester.id")
  ReportResponse toResponse(ReportRequest entity);

  List<ReportResponse> toList(List<ReportRequest> entities);

  ReportPage toPage(List<ReportResponse> content, int number, int size, long totalElements,
      int totalPages);

  default ReportPage toPage(Page<ReportRequest> page) {
    return toPage(toList(page.getContent()), page.getNumber(), page.getSize(),
        page.getTotalElements(), page.getTotalPages());
  }
}
