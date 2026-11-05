package com.bethelreport.backend.publications.mappers;

import com.bethelreport.backend.publications.dtos.AttachmentResponse;
import com.bethelreport.backend.publications.dtos.PublicationResponse;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.publications.entities.PublicationAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PublicationMapper {
  @Mapping(target = "authorId", source = "author.id")
  @Mapping(target = "bethelId", source = "bethel.id")
  PublicationResponse toResponse(Publication publication);

  AttachmentResponse toResponse(PublicationAttachment attachment);
}
