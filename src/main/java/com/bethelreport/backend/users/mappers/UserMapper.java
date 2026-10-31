package com.bethelreport.backend.users.mappers;

import com.bethelreport.backend.users.dtos.CreateUserRequest;
import com.bethelreport.backend.users.dtos.UpdateUserRequest;
import com.bethelreport.backend.users.dtos.UserResponse;
import com.bethelreport.backend.users.entities.User;
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
