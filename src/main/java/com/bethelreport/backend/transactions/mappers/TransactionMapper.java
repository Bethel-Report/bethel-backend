package com.bethelreport.backend.transactions.mappers;

import com.bethelreport.backend.transactions.dtos.TransactionPage;
import com.bethelreport.backend.transactions.dtos.TransactionRequest;
import com.bethelreport.backend.transactions.dtos.TransactionResponse;
import com.bethelreport.backend.transactions.entities.FinancialTransaction;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransactionMapper {
  String TRIM_QUALIFIER = "trim";

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "bethel", ignore = true)
  @Mapping(target = "author", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "description", qualifiedByName = TRIM_QUALIFIER)
  FinancialTransaction toEntity(TransactionRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "bethel", ignore = true)
  @Mapping(target = "author", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "description", qualifiedByName = TRIM_QUALIFIER)
  void updateEntity(TransactionRequest request, @MappingTarget FinancialTransaction entity);

  @Mapping(target = "bethelId", source = "bethel.id")
  @Mapping(target = "userId", source = "author.id")
  TransactionResponse toResponse(FinancialTransaction entity);

  List<TransactionResponse> toList(List<FinancialTransaction> entities);

  TransactionPage toPage(List<TransactionResponse> content, int number, int size,
      long totalElements, int totalPages);

  default TransactionPage toPage(Page<FinancialTransaction> page) {
    return toPage(toList(page.getContent()), page.getNumber(), page.getSize(),
        page.getTotalElements(), page.getTotalPages());
  }

  @Named(TRIM_QUALIFIER)
  default String trim(String value) {
    return value == null ? null : value.trim();
  }
}
