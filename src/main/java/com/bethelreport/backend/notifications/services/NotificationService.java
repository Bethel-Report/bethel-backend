package com.bethelreport.backend.notifications.services;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.forms.entities.Form;
import com.bethelreport.backend.notifications.dtos.NotificationResponse;
import com.bethelreport.backend.notifications.entities.Notification;
import com.bethelreport.backend.notifications.entities.NotificationType;
import com.bethelreport.backend.notifications.entities.UserNotification;
import com.bethelreport.backend.notifications.mappers.NotificationMapper;
import com.bethelreport.backend.notifications.repositories.NotificationRepository;
import com.bethelreport.backend.notifications.repositories.UserNotificationRepository;
import com.bethelreport.backend.publications.entities.Publication;
import com.bethelreport.backend.reports.entities.Report;
import com.bethelreport.backend.users.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final UserNotificationRepository userNotificationRepository;
  private final NotificationMapper mapper;
  private final EntityManager entityManager;

  public NotificationService(NotificationRepository notificationRepository,
      UserNotificationRepository userNotificationRepository, NotificationMapper mapper,
      EntityManager entityManager) {
    this.notificationRepository = notificationRepository;
    this.userNotificationRepository = userNotificationRepository;
    this.mapper = mapper;
    this.entityManager = entityManager;
  }

  public Page<NotificationResponse> list(Authentication authentication, int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new IllegalArgumentException("Página deve ser >= 0 e tamanho entre 1 e 100.");
    }
    String email = authenticatedEmail(authentication);
    return userNotificationRepository.findPageByUserEmail(email, PageRequest.of(page, size))
        .map(mapper::toResponse);
  }

  public NotificationResponse findById(Long id, Authentication authentication) {
    String email = authenticatedEmail(authentication);
    return findOwned(id, email);
  }

  @Transactional
  public NotificationResponse markAsRead(Long id, Authentication authentication) {
    String email = authenticatedEmail(authentication);
    UserNotification userNotification = findOwnedEntity(id, email);
    if (!userNotification.isSeen()) {
      userNotification.setSeen(true);
      userNotification.setReadAt(LocalDateTime.now());
      userNotificationRepository.save(userNotification);
    }
    return mapper.toResponse(userNotification);
  }

  public long countUnread(Authentication authentication) {
    return userNotificationRepository.countByUser_EmailAndSeenFalse(
        authenticatedEmail(authentication));
  }

  @Transactional
  public void notifyPublication(Publication publication) {
    List<Long> recipientIds = notificationRepository
        .findPublicationRecipientIds(publication.getId());
    createForUsers("Nova publicação: " + publication.getTitle(),
        "Uma nova publicação está disponível.", NotificationType.PUBLICATION,
        publication.getId(), publication.getBethel(), recipientIds);
  }

  @Transactional
  public void notifyFormPublished(Form form) {
    List<Long> recipientIds = notificationRepository.findActiveUserRecipientIds();
    createForUsers("Novo formulário disponível: " + form.getName(),
        "Um novo formulário foi publicado e está disponível para preenchimento.",
        NotificationType.FORM, form.getId(), null, recipientIds);
  }

  @Transactional
  public void notifyReportSubmitted(Report report) {
    List<Long> recipientIds = notificationRepository.findGrandeConselhoRecipientIds();
    createForUsers("Relatório enviado para revisão",
        "O relatório do Bethel " + report.getBethel().getName() + " foi submetido.",
        NotificationType.REPORT, report.getId(), report.getBethel(), recipientIds);
  }

  @Transactional
  public void notifyReportReopened(Report report) {
    Long bethelId = report.getBethel().getId();
    List<Long> recipientIds = notificationRepository.findBethelCouncilRecipientIds(bethelId);
    createForUsers("Relatório reaberto para ajustes",
        "O relatório do Bethel " + report.getBethel().getName()
            + " foi reaberto e precisa de ajustes.",
        NotificationType.REPORT, report.getId(), report.getBethel(), recipientIds);
  }

  private void createForUsers(String title, String content, NotificationType sourceType,
      Long resourceId, Bethel bethel, List<Long> recipientIds) {
    if (recipientIds.isEmpty()) {
      return;
    }
    Notification notification = new Notification();
    notification.setTitle(title);
    notification.setContent(content);
    notification.setSourceType(sourceType);
    notification.setResourceId(resourceId);
    notification.setBethel(bethel);
    notification.setCreatedAt(LocalDateTime.now());
    notificationRepository.save(notification);

    List<UserNotification> associations = recipientIds.stream().distinct().map(userId -> {
      UserNotification association = new UserNotification();
      association.setUser(entityManager.getReference(User.class, userId));
      association.setNotification(notification);
      association.setSeen(false);
      return association;
    }).toList();
    userNotificationRepository.saveAll(associations);
  }

  private NotificationResponse findOwned(Long id, String email) {
    return mapper.toResponse(findOwnedEntity(id, email));
  }

  private UserNotification findOwnedEntity(Long id, String email) {
    return userNotificationRepository.findOwnedById(id, email).orElseThrow(() -> {
      if (userNotificationRepository.existsById(id)) {
        return new AccessDeniedException("Esta notificação pertence a outro usuário.");
      }
      return new EntityNotFoundException("Notificação não encontrada.");
    });
  }

  private String authenticatedEmail(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwt)) {
      throw new AccessDeniedException("Autenticação necessária.");
    }
    String email = jwt.getToken().getClaimAsString("email");
    if (email == null || email.isBlank()) {
      throw new AccessDeniedException("Usuário não identificado.");
    }
    return email;
  }
}
