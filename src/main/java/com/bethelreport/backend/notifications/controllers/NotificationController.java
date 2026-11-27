package com.bethelreport.backend.notifications.controllers;

import com.bethelreport.backend.notifications.dtos.NotificationResponse;
import com.bethelreport.backend.notifications.services.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public Page<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size, Authentication authentication) {
    return notificationService.list(authentication, page, size);
  }

  @GetMapping("/unread-count")
  public long countUnread(Authentication authentication) {
    return notificationService.countUnread(authentication);
  }

  @GetMapping("/{id}")
  public NotificationResponse findById(@PathVariable Long id, Authentication authentication) {
    return notificationService.findById(id, authentication);
  }

  @PatchMapping("/{id}/read")
  @ResponseStatus(HttpStatus.OK)
  public NotificationResponse markAsRead(@PathVariable Long id, Authentication authentication) {
    return notificationService.markAsRead(id, authentication);
  }
}
