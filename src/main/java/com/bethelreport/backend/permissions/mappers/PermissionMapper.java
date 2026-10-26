package com.bethelreport.backend.permissions.mappers;

import com.bethelreport.backend.permissions.dtos.CreatePermissionRequest;
import com.bethelreport.backend.permissions.dtos.PermissionResponse;
import com.bethelreport.backend.permissions.dtos.UpdatePermissionRequest;
import com.bethelreport.backend.permissions.entities.Permission;
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

  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "bethelId", source = "bethel.id")
  PermissionResponse toResponse(Permission permission);
}
