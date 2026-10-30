package com.bethelreport.backend.bethel.mappers;

import com.bethelreport.backend.bethel.dtos.BethelResponse;
import com.bethelreport.backend.bethel.dtos.ValidateBethelRequest;
import com.bethelreport.backend.bethel.entities.Bethel;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface BethelMapper {

  String TRIM_QUALIFIER = "trim";

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", constant = "true")
  @Mapping(target = "name", qualifiedByName = TRIM_QUALIFIER)
  @Mapping(target = "city", qualifiedByName = TRIM_QUALIFIER)
  @Mapping(target = "address", qualifiedByName = TRIM_QUALIFIER)
  @Mapping(target = "email", qualifiedByName = TRIM_QUALIFIER)
  @Mapping(target = "sponsoringLodge", qualifiedByName = TRIM_QUALIFIER)
  Bethel toEntity(ValidateBethelRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "name", qualifiedByName = TRIM_QUALIFIER)
  @Mapping(target = "city", qualifiedByName = TRIM_QUALIFIER)
  @Mapping(target = "address", qualifiedByName = TRIM_QUALIFIER)
  @Mapping(target = "email", qualifiedByName = TRIM_QUALIFIER)
  @Mapping(target = "sponsoringLodge", qualifiedByName = TRIM_QUALIFIER)
  void updateEntity(ValidateBethelRequest request, @MappingTarget Bethel bethel);

  BethelResponse toResponse(Bethel bethel);

  List<BethelResponse> toList(List<Bethel> bethels);

  @Named(TRIM_QUALIFIER)
  default String trim(String value) {
    return value == null ? null : value.trim();
  }

}
