package com.bethelreport.backend.users.mappers;

import com.bethelreport.backend.users.dtos.CreatePermissionRequest;
import com.bethelreport.backend.users.dtos.UpdatePermissionRequest;
import com.bethelreport.backend.users.entities.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "bethel", ignore = true)
  @Mapping(target = "active", constant = "true")
  Permission toEntity(CreatePermissionRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "bethel", ignore = true)
  @Mapping(target = "active", ignore = true)
  void updateEntity(UpdatePermissionRequest request, @MappingTarget Permission permission);
}
