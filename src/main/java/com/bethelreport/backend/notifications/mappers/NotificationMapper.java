package com.bethelreport.backend.notifications.mappers;

import com.bethelreport.backend.notifications.dtos.NotificationResponse;
import com.bethelreport.backend.notifications.entities.UserNotification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationMapper {
  @Mapping(target = "id", source = "notification.id")
  @Mapping(target = "title", source = "notification.title")
  @Mapping(target = "content", source = "notification.content")
  @Mapping(target = "sourceType", source = "notification.sourceType")
  @Mapping(target = "resourceId", source = "notification.resourceId")
  @Mapping(target = "bethelId", source = "notification.bethel.id")
  @Mapping(target = "createdAt", source = "notification.createdAt")
  @Mapping(target = "seen", source = "seen")
  @Mapping(target = "readAt", source = "readAt")
  NotificationResponse toResponse(UserNotification userNotification);
}
