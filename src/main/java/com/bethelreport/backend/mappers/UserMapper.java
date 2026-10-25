package com.bethelreport.backend.mappers;

import com.bethelreport.backend.dtos.CreateUserRequest;
import com.bethelreport.backend.dtos.UpdateUserRequest;
import com.bethelreport.backend.dtos.UserResponse;
import com.bethelreport.backend.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", constant = "true")
  User toEntity(CreateUserRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", ignore = true)
  void updateEntity(UpdateUserRequest request, @MappingTarget User user);

  UserResponse toResponse(User user);
}
