package com.bethelreport.backend.notifications.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bethelreport.backend.forms.entities.Form;
import com.bethelreport.backend.notifications.dtos.NotificationResponse;
import com.bethelreport.backend.notifications.entities.Notification;
import com.bethelreport.backend.notifications.entities.NotificationType;
import com.bethelreport.backend.notifications.entities.UserNotification;
import com.bethelreport.backend.notifications.mappers.NotificationMapper;
import com.bethelreport.backend.notifications.repositories.NotificationRepository;
import com.bethelreport.backend.notifications.repositories.UserNotificationRepository;
import com.bethelreport.backend.users.entities.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private UserNotificationRepository userNotificationRepository;
  @Mock
  private EntityManager entityManager;

  private NotificationService service;

  @BeforeEach
  void setUp() {
    service = new NotificationService(notificationRepository, userNotificationRepository,
        Mappers.getMapper(NotificationMapper.class), entityManager);
  }

  @Test
  void listsOnlyAssociationsForAuthenticatedUserWithPagination() {
    UserNotification association = association(1L, "member@example.test", false);
    when(userNotificationRepository.findPageByUserEmail(
        org.mockito.ArgumentMatchers.eq("member@example.test"),
        org.mockito.ArgumentMatchers.any())).thenReturn(new PageImpl<>(List.of(association)));

    var page = service.list(authentication("member@example.test"), 0, 10);

    assertEquals(1, page.getTotalElements());
    assertEquals("Aviso", page.getContent().getFirst().title());
    verify(userNotificationRepository).findPageByUserEmail(
        org.mockito.ArgumentMatchers.eq("member@example.test"), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void marksOwnedNotificationAsRead() {
    UserNotification association = association(4L, "member@example.test", false);
    when(userNotificationRepository.findOwnedById(4L, "member@example.test"))
        .thenReturn(Optional.of(association));

    NotificationResponse result = service.markAsRead(4L, authentication("member@example.test"));

    assertTrue(result.seen());
    assertNotNull(result.readAt());
    verify(userNotificationRepository).save(association);
  }

  @Test
  void rejectsReadingNotificationOwnedByAnotherUser() {
    when(userNotificationRepository.findOwnedById(9L, "member@example.test"))
        .thenReturn(Optional.empty());
    when(userNotificationRepository.existsById(9L)).thenReturn(true);

    assertThrows(AccessDeniedException.class,
        () -> service.markAsRead(9L, authentication("member@example.test")));
  }

  @Test
  void countsUnreadNotificationsForAuthenticatedUser() {
    when(userNotificationRepository.countByUser_EmailAndSeenFalse("member@example.test"))
        .thenReturn(3L);

    assertEquals(3L, service.countUnread(authentication("member@example.test")));
  }

  @Test
  @SuppressWarnings("unchecked")
  void persistsNotificationAndRecipientAssociationsForPublishedForm() {
    Form form = new Form();
    form.setId(20L);
    form.setName("Relatório mensal");
    when(notificationRepository.findActiveUserRecipientIds()).thenReturn(List.of(1L, 2L));
    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(entityManager.getReference(User.class, 1L)).thenReturn(new User());
    when(entityManager.getReference(User.class, 2L)).thenReturn(new User());

    service.notifyFormPublished(form);

    ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(notificationCaptor.capture());
    assertEquals(NotificationType.FORM, notificationCaptor.getValue().getSourceType());
    assertEquals(20L, notificationCaptor.getValue().getResourceId());
    assertEquals("Novo formulário disponível: Relatório mensal",
        notificationCaptor.getValue().getTitle());

    ArgumentCaptor<List<UserNotification>> associationsCaptor = ArgumentCaptor.forClass(List.class);
    verify(userNotificationRepository).saveAll(associationsCaptor.capture());
    assertEquals(2, associationsCaptor.getValue().size());
    assertFalse(associationsCaptor.getValue().getFirst().isSeen());
    assertTrue(associationsCaptor.getValue().stream()
        .allMatch(item -> item.getNotification() == notificationCaptor.getValue()));
  }

  private UserNotification association(Long id, String email, boolean seen) {
    Notification notification = new Notification();
    notification.setId(id);
    notification.setTitle("Aviso");
    notification.setContent("Conteúdo do aviso");
    notification.setSourceType(NotificationType.PUBLICATION);
    notification.setResourceId(12L);
    notification.setCreatedAt(LocalDateTime.now());

    User user = new User();
    user.setEmail(email);
    UserNotification association = new UserNotification();
    association.setId(id);
    association.setNotification(notification);
    association.setUser(user);
    association.setSeen(seen);
    return association;
  }

  private JwtAuthenticationToken authentication(String email) {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("email", email)
        .build();
    return new JwtAuthenticationToken(jwt);
  }
}
