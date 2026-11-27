package com.bethelreport.backend.reports.services;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.forms.entities.Form;
import com.bethelreport.backend.forms.entities.FormField;
import com.bethelreport.backend.forms.entities.FormStatus;
import com.bethelreport.backend.forms.repositories.FormRepository;
import com.bethelreport.backend.notifications.services.NotificationService;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.reports.dtos.CreateReportRequest;
import com.bethelreport.backend.reports.dtos.ReopenReportRequest;
import com.bethelreport.backend.reports.dtos.ReportAnswerRequest;
import com.bethelreport.backend.reports.dtos.ReportResponse;
import com.bethelreport.backend.reports.dtos.SaveAnswersRequest;
import com.bethelreport.backend.reports.entities.Report;
import com.bethelreport.backend.reports.entities.ReportAnswer;
import com.bethelreport.backend.reports.entities.ReportStatus;
import com.bethelreport.backend.reports.mappers.ReportMapper;
import com.bethelreport.backend.reports.repositories.ReportRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class ReportService {
  private final ReportRepository reportRepository;
  private final FormRepository formRepository;
  private final BethelRepository bethelRepository;
  private final ReportMapper reportMapper;
  private final AuthorizationService authorizationService;
  private NotificationService notificationService;

  public ReportService(ReportRepository reportRepository, FormRepository formRepository,
      BethelRepository bethelRepository, ReportMapper reportMapper,
      AuthorizationService authorizationService) {
    this.reportRepository = reportRepository;
    this.formRepository = formRepository;
    this.bethelRepository = bethelRepository;
    this.reportMapper = reportMapper;
    this.authorizationService = authorizationService;
  }

  @Autowired(required = false)
  public void setNotificationService(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  public List<ReportResponse> findAll(Authentication authentication) {
    if (authorizationService.isGrandeConselho(authentication)) {
      return reportMapper.toResponseList(reportRepository.findAllByOrderByCreatedAtDesc());
    }
    Long bethelId = authorizationService.getAuthorizedBethelId(authentication);
    return reportMapper
        .toResponseList(reportRepository.findAllByBethelIdOrderByCreatedAtDesc(bethelId));
  }

  public ReportResponse findById(Long id, Authentication authentication) {
    Report report = findWithAnswers(id);
    checkAccess(report, authentication);
    return reportMapper.toResponse(report);
  }

  @Transactional
  public ReportResponse create(CreateReportRequest request, Authentication authentication) {
    Long bethelId = authorizationService.getAuthorizedBethelId(authentication);

    Form form = formRepository.findById(request.formId())
        .orElseThrow(() -> new EntityNotFoundException("Formulário não encontrado."));

    if (form.getStatus() != FormStatus.PUBLISHED) {
      throw new IllegalStateException("Somente formulários publicados podem receber relatórios.");
    }
    ReportValidation.requireFormOpen(form);

    reportRepository.findByBethelIdAndFormId(bethelId, form.getId()).ifPresent(existing -> {
      throw new IllegalStateException("Relatório já existe para este Bethel e formulário.");
    });

    Bethel bethel = bethelRepository.findById(bethelId)
        .orElseThrow(() -> new EntityNotFoundException("Bethel não encontrado."));

    Report report = new Report();
    report.setBethel(bethel);
    report.setForm(form);
    report.setStatus(ReportStatus.DRAFT);
    report.setCreatedAt(LocalDateTime.now());
    report.setUpdatedAt(LocalDateTime.now());

    return reportMapper.toResponse(reportRepository.save(report));
  }

  @Transactional
  public ReportResponse saveAnswers(Long id, SaveAnswersRequest request,
      Authentication authentication) {
    Report report = findWithAnswers(id);
    checkOwnership(report, authentication);
    requireEditable(report);
    if (report.getStatus() != ReportStatus.REOPENED) {
      ReportValidation.requireFormOpen(report.getForm());
    }

    Set<Long> uniqueFieldIds = request.answers().stream().map(ReportAnswerRequest::fieldId)
        .collect(Collectors.toSet());
    if (uniqueFieldIds.size() != request.answers().size()) {
      throw new IllegalArgumentException("A requisição contém campos duplicados.");
    }

    Map<Long, FormField> fieldMap = report.getForm().getFields().stream()
        .collect(Collectors.toMap(FormField::getId, Function.identity()));

    Map<Long, ReportAnswer> existingAnswers = report.getAnswers().stream()
        .collect(Collectors.toMap(a -> a.getField().getId(), Function.identity()));

    for (ReportAnswerRequest answerReq : request.answers()) {
      FormField field = fieldMap.get(answerReq.fieldId());
      if (field == null) {
        throw new IllegalArgumentException(
            "Campo " + answerReq.fieldId() + " não pertence ao formulário.");
      }

      ReportValidation.validateAnswerValue(field, answerReq.value());

      ReportAnswer answer = existingAnswers.get(answerReq.fieldId());
      if (answer == null) {
        answer = createAnswer(report, field);
        report.getAnswers().add(answer);
      }
      answer.setValue(answerReq.value());
      answer.setUpdatedAt(LocalDateTime.now());
    }

    report.setUpdatedAt(LocalDateTime.now());
    return reportMapper.toResponse(reportRepository.save(report));
  }

  @Transactional
  public ReportResponse submit(Long id, Authentication authentication) {
    Report report = findWithAnswers(id);
    checkOwnership(report, authentication);
    requireEditable(report);
    if (report.getStatus() != ReportStatus.REOPENED) {
      ReportValidation.requireFormOpen(report.getForm());
    }

    Set<Long> answeredFieldIds = report.getAnswers().stream()
        .filter(a -> a.getValue() != null && !a.getValue().isBlank()).map(a -> a.getField().getId())
        .collect(Collectors.toSet());

    report.getForm().getFields().stream().filter(FormField::isRequired).forEach(field -> {
      if (!answeredFieldIds.contains(field.getId())) {
        throw new IllegalStateException("Campo obrigatório não preenchido: " + field.getLabel());
      }
    });

    report.setStatus(ReportStatus.SUBMITTED);
    report.setSubmittedAt(LocalDateTime.now());
    report.setUpdatedAt(LocalDateTime.now());
    Report submitted = reportRepository.save(report);
    if (notificationService != null) {
      notificationService.notifyReportSubmitted(submitted);
    }
    return reportMapper.toResponse(submitted);
  }

  @Transactional
  public ReportResponse reopen(Long id, ReopenReportRequest request) {
    Report report = findOrThrow(id);
    if (report.getStatus() != ReportStatus.SUBMITTED) {
      throw new IllegalStateException("Somente relatórios submetidos podem ser reabertos.");
    }
    if (!request.reopenDeadline().isAfter(LocalDateTime.now())) {
      throw new IllegalArgumentException("O prazo para reenvio deve ser uma data futura.");
    }

    report.setStatus(ReportStatus.REOPENED);
    report.setReopenedAt(LocalDateTime.now());
    report.setReopenDeadline(request.reopenDeadline());
    report.setUpdatedAt(LocalDateTime.now());
    Report reopened = reportRepository.save(report);
    if (notificationService != null) {
      notificationService.notifyReportReopened(reopened);
    }
    return reportMapper.toResponse(reopened);
  }

  private Report findOrThrow(Long id) {
    return reportRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Relatório não encontrado."));
  }

  private Report findWithAnswers(Long id) {
    return reportRepository.findByIdWithAnswers(id)
        .orElseThrow(() -> new EntityNotFoundException("Relatório não encontrado."));
  }

  private void checkAccess(Report report, Authentication authentication) {
    if (!authorizationService.canAccessBethel(report.getBethel().getId(), authentication)) {
      throw new AccessDeniedException("Sem acesso a este relatório.");
    }
  }

  private void checkOwnership(Report report, Authentication authentication) {
    if (!authorizationService.canManageBethel(report.getBethel().getId(), authentication)) {
      throw new AccessDeniedException("Sem permissão para gerenciar este relatório.");
    }
  }

  private ReportAnswer createAnswer(Report report, FormField field) {
    ReportAnswer answer = new ReportAnswer();
    answer.setReport(report);
    answer.setField(field);
    return answer;
  }

  private void requireEditable(Report report) {
    if (report.getStatus() == ReportStatus.SUBMITTED) {
      throw new IllegalStateException("Relatório já submetido não pode ser alterado.");
    }
    if (report.getStatus() == ReportStatus.REOPENED && report.getReopenDeadline() != null
        && LocalDateTime.now().isAfter(report.getReopenDeadline())) {
      throw new IllegalStateException("Prazo para reenvio expirado.");
    }
  }
}
