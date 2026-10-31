package com.bethelreport.backend.invitations.mappers;

import com.bethelreport.backend.invitations.dtos.CreateInvitationRequest;
import com.bethelreport.backend.invitations.dtos.InvitationResponse;
import com.bethelreport.backend.invitations.entities.Invitation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvitationMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "bethel", ignore = true)
  Invitation toEntity(CreateInvitationRequest request);

  @Mapping(target = "bethelId", source = "bethel.id")
  InvitationResponse toResponse(Invitation invitation);
}
