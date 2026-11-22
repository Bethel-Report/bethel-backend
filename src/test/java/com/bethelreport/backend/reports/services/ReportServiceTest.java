package com.bethelreport.backend.reports.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.forms.entities.Form;
import com.bethelreport.backend.forms.entities.FormField;
import com.bethelreport.backend.forms.entities.FormFieldType;
import com.bethelreport.backend.forms.entities.FormStatus;
import com.bethelreport.backend.forms.repositories.FormRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

class ReportServiceTest {
  private ReportRepository reportRepository;
  private FormRepository formRepository;
  private BethelRepository bethelRepository;
  private AuthorizationService authorizationService;
  private ReportService service;
  private Authentication authentication;

  @BeforeEach
  void setUp() {
    reportRepository = mock(ReportRepository.class);
    formRepository = mock(FormRepository.class);
    bethelRepository = mock(BethelRepository.class);
    ReportMapper mapper = Mappers.getMapper(ReportMapper.class);
    authorizationService = mock(AuthorizationService.class);
    service = new ReportService(reportRepository, formRepository, bethelRepository, mapper,
        authorizationService);
    authentication = mock(Authentication.class);
  }

  @Test
  void createsReportForPublishedForm() {
    when(authorizationService.getAuthorizedBethelId(authentication)).thenReturn(10L);
    when(formRepository.findById(1L)).thenReturn(Optional.of(publishedForm()));
    when(reportRepository.findByBethelIdAndFormId(10L, 1L)).thenReturn(Optional.empty());
    when(bethelRepository.findById(10L)).thenReturn(Optional.of(bethel()));
    saveReturnsArgument();

    ReportResponse result = service.create(new CreateReportRequest(1L), authentication);

    assertEquals(ReportStatus.DRAFT, result.status());
    assertEquals(10L, result.bethelId());
    assertEquals("Bethel 1", result.bethelName());
    assertEquals(1L, result.formId());
    assertEquals("Relatório Anual 2026", result.formName());
    assertNotNull(result.createdAt());
    verify(reportRepository).save(any(Report.class));
  }

  @Test
  void rejectsReportForDraftForm() {
    when(authorizationService.getAuthorizedBethelId(authentication)).thenReturn(10L);
    Form draft = publishedForm();
    draft.setStatus(FormStatus.DRAFT);
    when(formRepository.findById(1L)).thenReturn(Optional.of(draft));

    assertThrows(IllegalStateException.class,
        () -> service.create(new CreateReportRequest(1L), authentication));
  }

  @Test
  void rejectsDuplicateReport() {
    when(authorizationService.getAuthorizedBethelId(authentication)).thenReturn(10L);
    when(formRepository.findById(1L)).thenReturn(Optional.of(publishedForm()));
    when(reportRepository.findByBethelIdAndFormId(10L, 1L)).thenReturn(Optional.of(draftReport()));

    assertThrows(IllegalStateException.class,
        () -> service.create(new CreateReportRequest(1L), authentication));
  }

  @Test
  void savesAnswersForDraftReport() {
    Report report = draftReport();
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);
    saveReturnsArgument();

    SaveAnswersRequest request = new SaveAnswersRequest(List.of(new ReportAnswerRequest(1L, "42")));
    ReportResponse result = service.saveAnswers(1L, request, authentication);

    assertEquals(1, result.answers().size());
    assertEquals("42", result.answers().get(0).value());
    assertEquals(1L, result.answers().get(0).fieldId());
    assertEquals("Reuniões realizadas", result.answers().get(0).fieldLabel());
  }

  @Test
  void updatesExistingAnswer() {
    Report report = draftReport();
    ReportAnswer existing = new ReportAnswer();
    existing.setId(1L);
    existing.setReport(report);
    existing.setField(report.getForm().getFields().get(0));
    existing.setValue("10");
    existing.setUpdatedAt(LocalDateTime.now());
    report.getAnswers().add(existing);

    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);
    saveReturnsArgument();

    SaveAnswersRequest request = new SaveAnswersRequest(List.of(new ReportAnswerRequest(1L, "20")));
    ReportResponse result = service.saveAnswers(1L, request, authentication);

    assertEquals(1, result.answers().size());
    assertEquals("20", result.answers().get(0).value());
  }

  @Test
  void rejectsAnswerForInvalidField() {
    Report report = draftReport();
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    SaveAnswersRequest request = new SaveAnswersRequest(
        List.of(new ReportAnswerRequest(999L, "valor")));
    assertThrows(IllegalArgumentException.class,
        () -> service.saveAnswers(1L, request, authentication));
  }

  @Test
  void rejectsAnswersForSubmittedReport() {
    Report report = draftReport();
    report.setStatus(ReportStatus.SUBMITTED);
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    SaveAnswersRequest request = new SaveAnswersRequest(List.of(new ReportAnswerRequest(1L, "42")));
    assertThrows(IllegalStateException.class,
        () -> service.saveAnswers(1L, request, authentication));
  }

  @Test
  void submitsReportWithAllRequiredFields() {
    Report report = draftReport();
    ReportAnswer answer = new ReportAnswer();
    answer.setId(1L);
    answer.setReport(report);
    answer.setField(report.getForm().getFields().get(0));
    answer.setValue("42");
    report.getAnswers().add(answer);

    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);
    saveReturnsArgument();

    ReportResponse result = service.submit(1L, authentication);

    assertEquals(ReportStatus.SUBMITTED, result.status());
    assertNotNull(result.submittedAt());
  }

  @Test
  void rejectsSubmitWithMissingRequiredField() {
    Report report = draftReport();
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    assertThrows(IllegalStateException.class, () -> service.submit(1L, authentication));
  }

  @Test
  void reopensSubmittedReport() {
    Report report = draftReport();
    report.setStatus(ReportStatus.SUBMITTED);
    when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
    saveReturnsArgument();

    LocalDateTime deadline = LocalDateTime.now().plusDays(7);
    ReportResponse result = service.reopen(1L, new ReopenReportRequest(deadline));

    assertEquals(ReportStatus.REOPENED, result.status());
    assertNotNull(result.reopenedAt());
    assertEquals(deadline, result.reopenDeadline());
  }

  @Test
  void rejectsReopenOfDraftReport() {
    Report report = draftReport();
    when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

    assertThrows(IllegalStateException.class,
        () -> service.reopen(1L, new ReopenReportRequest(LocalDateTime.now().plusDays(7))));
  }

  @Test
  void listsAllReportsForGrandeConselho() {
    when(authorizationService.isGrandeConselho(authentication)).thenReturn(true);
    when(reportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(draftReport()));

    List<ReportResponse> result = service.findAll(authentication);

    assertEquals(1, result.size());
    assertEquals("Bethel 1", result.get(0).bethelName());
  }

  @Test
  void listsBethelReportsForConselho() {
    when(authorizationService.isGrandeConselho(authentication)).thenReturn(false);
    when(authorizationService.getAuthorizedBethelId(authentication)).thenReturn(10L);
    when(reportRepository.findAllByBethelIdOrderByCreatedAtDesc(10L))
        .thenReturn(List.of(draftReport()));

    List<ReportResponse> result = service.findAll(authentication);

    assertEquals(1, result.size());
  }

  @Test
  void findsReportById() {
    Report report = draftReport();
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canAccessBethel(10L, authentication)).thenReturn(true);

    ReportResponse result = service.findById(1L, authentication);

    assertEquals(1L, result.id());
  }

  @Test
  void throwsWhenReportNotFound() {
    when(reportRepository.findByIdWithAnswers(99L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> service.findById(99L, authentication));
  }

  @Test
  void rejectsAccessToOtherBethelsReport() {
    Report report = draftReport();
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canAccessBethel(10L, authentication)).thenReturn(false);

    assertThrows(AccessDeniedException.class, () -> service.findById(1L, authentication));
  }

  @Test
  void rejectsAnswersWhenNotOwner() {
    Report report = draftReport();
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(false);

    SaveAnswersRequest request = new SaveAnswersRequest(List.of(new ReportAnswerRequest(1L, "42")));
    assertThrows(AccessDeniedException.class,
        () -> service.saveAnswers(1L, request, authentication));
  }

  @Test
  void savesAnswersForReopenedReport() {
    Report report = draftReport();
    report.setStatus(ReportStatus.REOPENED);
    report.setReopenDeadline(LocalDateTime.now().plusDays(7));
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);
    saveReturnsArgument();

    SaveAnswersRequest request = new SaveAnswersRequest(List.of(new ReportAnswerRequest(1L, "42")));
    ReportResponse result = service.saveAnswers(1L, request, authentication);

    assertEquals(1, result.answers().size());
  }

  @Test
  void rejectsReopenWithPastDeadline() {
    Report report = draftReport();
    report.setStatus(ReportStatus.SUBMITTED);
    when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

    assertThrows(IllegalArgumentException.class,
        () -> service.reopen(1L, new ReopenReportRequest(LocalDateTime.now().minusDays(1))));
  }

  @Test
  void rejectsNonNumericAnswerForNumberField() {
    Report report = draftReport();
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    SaveAnswersRequest request = new SaveAnswersRequest(
        List.of(new ReportAnswerRequest(1L, "abc")));
    assertThrows(IllegalArgumentException.class,
        () -> service.saveAnswers(1L, request, authentication));
  }

  @Test
  void rejectsInvalidDateAnswer() {
    Report report = draftReport();
    addField(report.getForm(), 2L, "Data do evento", FormFieldType.DATE, false);
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    SaveAnswersRequest request = new SaveAnswersRequest(
        List.of(new ReportAnswerRequest(2L, "data-invalida")));
    assertThrows(IllegalArgumentException.class,
        () -> service.saveAnswers(1L, request, authentication));
  }

  @Test
  void rejectsInvalidBooleanAnswer() {
    Report report = draftReport();
    addField(report.getForm(), 2L, "Ativo", FormFieldType.BOOLEAN, false);
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    SaveAnswersRequest request = new SaveAnswersRequest(
        List.of(new ReportAnswerRequest(2L, "talvez")));
    assertThrows(IllegalArgumentException.class,
        () -> service.saveAnswers(1L, request, authentication));
  }

  @Test
  void rejectsInvalidSelectAnswer() {
    Report report = draftReport();
    FormField selectField = new FormField();
    selectField.setId(2L);
    selectField.setForm(report.getForm());
    selectField.setLabel("Categoria");
    selectField.setType(FormFieldType.SELECT);
    selectField.setRequired(false);
    selectField.setDisplayOrder(2);
    selectField.setOptions(new ArrayList<>(List.of("A", "B", "C")));
    report.getForm().getFields().add(selectField);

    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    SaveAnswersRequest request = new SaveAnswersRequest(List.of(new ReportAnswerRequest(2L, "D")));
    assertThrows(IllegalArgumentException.class,
        () -> service.saveAnswers(1L, request, authentication));
  }

  @Test
  void rejectsCreateWhenFormNotYetOpen() {
    when(authorizationService.getAuthorizedBethelId(authentication)).thenReturn(10L);
    Form form = publishedForm();
    form.setOpeningDate(LocalDate.now().plusDays(10));
    when(formRepository.findById(1L)).thenReturn(Optional.of(form));

    assertThrows(IllegalStateException.class,
        () -> service.create(new CreateReportRequest(1L), authentication));
  }

  @Test
  void rejectsCreateWhenFormAlreadyClosed() {
    when(authorizationService.getAuthorizedBethelId(authentication)).thenReturn(10L);
    Form form = publishedForm();
    form.setClosingDate(LocalDate.now().minusDays(1));
    when(formRepository.findById(1L)).thenReturn(Optional.of(form));

    assertThrows(IllegalStateException.class,
        () -> service.create(new CreateReportRequest(1L), authentication));
  }

  @Test
  void rejectsSaveAnswersWhenFormAlreadyClosed() {
    Report report = draftReport();
    report.getForm().setClosingDate(LocalDate.now().minusDays(1));
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    SaveAnswersRequest request = new SaveAnswersRequest(List.of(new ReportAnswerRequest(1L, "42")));
    assertThrows(IllegalStateException.class,
        () -> service.saveAnswers(1L, request, authentication));
  }

  @Test
  void rejectsDuplicateFieldIdsInRequest() {
    Report report = draftReport();
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    SaveAnswersRequest request = new SaveAnswersRequest(
        List.of(new ReportAnswerRequest(1L, "10"), new ReportAnswerRequest(1L, "20")));
    assertThrows(IllegalArgumentException.class,
        () -> service.saveAnswers(1L, request, authentication));
  }

  @Test
  void rejectsCreateWhenFormManuallyClosed() {
    when(authorizationService.getAuthorizedBethelId(authentication)).thenReturn(10L);
    Form form = publishedForm();
    form.setStatus(FormStatus.CLOSED);
    when(formRepository.findById(1L)).thenReturn(Optional.of(form));

    assertThrows(IllegalStateException.class,
        () -> service.create(new CreateReportRequest(1L), authentication));
  }

  @Test
  void rejectsSaveAnswersWhenFormManuallyClosed() {
    Report report = draftReport();
    report.getForm().setStatus(FormStatus.CLOSED);
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    SaveAnswersRequest request = new SaveAnswersRequest(List.of(new ReportAnswerRequest(1L, "42")));
    assertThrows(IllegalStateException.class,
        () -> service.saveAnswers(1L, request, authentication));
  }

  @Test
  void allowsSaveAnswersForReopenedReportAfterFormClosed() {
    Report report = draftReport();
    report.setStatus(ReportStatus.REOPENED);
    report.setReopenDeadline(LocalDateTime.now().plusDays(7));
    report.getForm().setClosingDate(LocalDate.now().minusDays(1));
    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);
    saveReturnsArgument();

    SaveAnswersRequest request = new SaveAnswersRequest(List.of(new ReportAnswerRequest(1L, "42")));
    ReportResponse result = service.saveAnswers(1L, request, authentication);

    assertEquals(1, result.answers().size());
  }

  @Test
  void allowsSubmitForReopenedReportAfterFormClosed() {
    Report report = draftReport();
    report.setStatus(ReportStatus.REOPENED);
    report.setReopenDeadline(LocalDateTime.now().plusDays(7));
    report.getForm().setClosingDate(LocalDate.now().minusDays(1));
    ReportAnswer answer = new ReportAnswer();
    answer.setId(1L);
    answer.setReport(report);
    answer.setField(report.getForm().getFields().get(0));
    answer.setValue("42");
    report.getAnswers().add(answer);

    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);
    saveReturnsArgument();

    ReportResponse result = service.submit(1L, authentication);

    assertEquals(ReportStatus.SUBMITTED, result.status());
  }

  @Test
  void rejectsSubmitWhenFormAlreadyClosed() {
    Report report = draftReport();
    report.getForm().setClosingDate(LocalDate.now().minusDays(1));
    ReportAnswer answer = new ReportAnswer();
    answer.setId(1L);
    answer.setReport(report);
    answer.setField(report.getForm().getFields().get(0));
    answer.setValue("42");
    report.getAnswers().add(answer);

    when(reportRepository.findByIdWithAnswers(1L)).thenReturn(Optional.of(report));
    when(authorizationService.canManageBethel(10L, authentication)).thenReturn(true);

    assertThrows(IllegalStateException.class, () -> service.submit(1L, authentication));
  }

  private Bethel bethel() {
    Bethel bethel = new Bethel();
    bethel.setId(10L);
    bethel.setName("Bethel 1");
    bethel.setNumber(1);
    bethel.setCity("São Paulo");
    bethel.setAddress("Rua A");
    bethel.setEmail("bethel@example.com");
    bethel.setSponsoringLodge("Loja 1");
    return bethel;
  }

  private Form publishedForm() {
    Form form = new Form();
    form.setId(1L);
    form.setName("Relatório Anual 2026");
    form.setStatus(FormStatus.PUBLISHED);
    form.setReferenceYear(2026);
    form.setOpeningDate(LocalDate.now().minusDays(30));
    form.setClosingDate(LocalDate.now().plusDays(30));
    addField(form, 1L, "Reuniões realizadas", FormFieldType.NUMBER, true);
    return form;
  }

  private void addField(Form form, Long fieldId, String label, FormFieldType type,
      boolean required) {
    FormField field = new FormField();
    field.setId(fieldId);
    field.setForm(form);
    field.setLabel(label);
    field.setType(type);
    field.setRequired(required);
    field.setDisplayOrder(form.getFields().size() + 1);
    field.setOptions(new ArrayList<>());
    form.getFields().add(field);
  }

  private Report draftReport() {
    Report report = new Report();
    report.setId(1L);
    report.setBethel(bethel());
    report.setForm(publishedForm());
    report.setStatus(ReportStatus.DRAFT);
    report.setCreatedAt(LocalDateTime.now());
    report.setUpdatedAt(LocalDateTime.now());
    return report;
  }

  private void saveReturnsArgument() {
    when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
      Report r = inv.getArgument(0);
      if (r.getId() == null) {
        r.setId(1L);
      }
      return r;
    });
  }
}
