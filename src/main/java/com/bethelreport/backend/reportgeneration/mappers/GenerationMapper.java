package com.bethelreport.backend.reportgeneration.mappers;

import com.bethelreport.backend.reportgeneration.dtos.GenerationPage;
import com.bethelreport.backend.reportgeneration.dtos.GenerationResponse;
import com.bethelreport.backend.reportgeneration.entities.GenerationRequest;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GenerationMapper {

  @Mapping(target = "bethelId", source = "bethel.id")
  @Mapping(target = "requesterId", source = "requester.id")
  @Mapping(target = "type", source = "generationType")
  GenerationResponse toResponse(GenerationRequest entity);

  List<GenerationResponse> toList(List<GenerationRequest> entities);

  GenerationPage toPage(List<GenerationResponse> content, int number, int size, long totalElements,
      int totalPages);

  default GenerationPage toPage(Page<GenerationRequest> page) {
    return toPage(toList(page.getContent()), page.getNumber(), page.getSize(),
        page.getTotalElements(), page.getTotalPages());
  }
}
