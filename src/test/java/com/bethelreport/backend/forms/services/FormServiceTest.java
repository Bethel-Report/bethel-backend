package com.bethelreport.backend.forms.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bethelreport.backend.forms.dtos.CloneFormRequest;
import com.bethelreport.backend.forms.dtos.CreateFormRequest;
import com.bethelreport.backend.forms.dtos.FormFieldRequest;
import com.bethelreport.backend.forms.dtos.FormResponse;
import com.bethelreport.backend.forms.dtos.UpdateFormRequest;
import com.bethelreport.backend.forms.entities.Form;
import com.bethelreport.backend.forms.entities.FormField;
import com.bethelreport.backend.forms.entities.FormFieldType;
import com.bethelreport.backend.forms.entities.FormStatus;
import com.bethelreport.backend.forms.mappers.FormMapper;
import com.bethelreport.backend.forms.repositories.FormRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class FormServiceTest {
  private FormRepository repository;
  private FormMapper mapper;
  private FormService service;

  @BeforeEach
  void setUp() {
    repository = mock(FormRepository.class);
    mapper = Mappers.getMapper(FormMapper.class);
    service = new FormService(repository, mapper);
  }

  @Test
  void createsFormWithFields() {
    saveReturnsArgument();
    FormResponse result = service.create(createRequest());
    assertEquals("Relatório Anual 2026", result.name());
    assertEquals(2026, result.referenceYear());
    assertEquals(FormStatus.DRAFT, result.status());
    assertEquals(1, result.fields().size());
    assertEquals("Reuniões realizadas", result.fields().get(0).label());
    assertNotNull(result.createdAt());
    verify(repository).save(any(Form.class));
  }

  @Test
  void createsFormWithSelectFieldOptions() {
    saveReturnsArgument();
    FormFieldRequest selectField = new FormFieldRequest("Categoria", FormFieldType.SELECT, true,
        null, null, null, null, 1, List.of("A", "B", "C"));
    CreateFormRequest request = new CreateFormRequest("Formulário", null, 2026,
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), List.of(selectField));
    FormResponse result = service.create(request);
    assertEquals(FormFieldType.SELECT, result.fields().get(0).type());
    assertEquals(List.of("A", "B", "C"), result.fields().get(0).options());
  }

  @Test
  void rejectsClosingDateBeforeOpeningDate() {
    CreateFormRequest request = new CreateFormRequest("Formulário", null, 2026,
        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), List.of(fieldRequest()));
    assertThrows(IllegalArgumentException.class, () -> service.create(request));
  }

  @Test
  void rejectsSelectFieldWithoutOptions() {
    FormFieldRequest selectField = new FormFieldRequest("Categoria", FormFieldType.SELECT, true,
        null, null, null, null, 1, null);
    CreateFormRequest request = new CreateFormRequest("Formulário", null, 2026, null, null,
        List.of(selectField));
    assertThrows(IllegalArgumentException.class, () -> service.create(request));
  }

  @Test
  void updatesDraftForm() {
    Form existing = draftForm(1L);
    when(repository.findById(1L)).thenReturn(Optional.of(existing));
    saveReturnsArgument();

    UpdateFormRequest request = new UpdateFormRequest("Nome Atualizado", "Descrição", 2026,
        LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 30),
        List.of(new FormFieldRequest("Campo Novo", FormFieldType.TEXT, false, null, null, null,
            null, 1, null)));

    FormResponse result = service.update(1L, request);
    assertEquals("Nome Atualizado", result.name());
    assertEquals("Campo Novo", result.fields().get(0).label());
  }

  @Test
  void rejectsUpdateOfPublishedForm() {
    Form published = draftForm(1L);
    published.setStatus(FormStatus.PUBLISHED);
    when(repository.findById(1L)).thenReturn(Optional.of(published));

    UpdateFormRequest request = new UpdateFormRequest("Novo Nome", null, 2026, null, null,
        List.of(fieldRequest()));
    assertThrows(IllegalStateException.class, () -> service.update(1L, request));
  }

  @Test
  void rejectsUpdateOfClosedForm() {
    Form closed = draftForm(1L);
    closed.setStatus(FormStatus.CLOSED);
    when(repository.findById(1L)).thenReturn(Optional.of(closed));

    UpdateFormRequest request = new UpdateFormRequest("Novo Nome", null, 2026, null, null,
        List.of(fieldRequest()));
    assertThrows(IllegalStateException.class, () -> service.update(1L, request));
  }

  @Test
  void publishesDraftForm() {
    Form form = draftForm(1L);
    form.setOpeningDate(LocalDate.of(2026, 1, 1));
    form.setClosingDate(LocalDate.of(2026, 3, 31));
    addField(form, "Campo 1", FormFieldType.TEXT);
    when(repository.findById(1L)).thenReturn(Optional.of(form));
    saveReturnsArgument();

    FormResponse result = service.publish(1L);
    assertEquals(FormStatus.PUBLISHED, result.status());
  }

  @Test
  void rejectsPublishWithoutFields() {
    Form form = draftForm(1L);
    form.setOpeningDate(LocalDate.of(2026, 1, 1));
    form.setClosingDate(LocalDate.of(2026, 3, 31));
    when(repository.findById(1L)).thenReturn(Optional.of(form));

    assertThrows(IllegalStateException.class, () -> service.publish(1L));
  }

  @Test
  void rejectsPublishWithoutDates() {
    Form form = draftForm(1L);
    addField(form, "Campo 1", FormFieldType.TEXT);
    when(repository.findById(1L)).thenReturn(Optional.of(form));

    assertThrows(IllegalStateException.class, () -> service.publish(1L));
  }

  @Test
  void closesPublishedForm() {
    Form form = draftForm(1L);
    form.setStatus(FormStatus.PUBLISHED);
    when(repository.findById(1L)).thenReturn(Optional.of(form));
    saveReturnsArgument();

    FormResponse result = service.close(1L);
    assertEquals(FormStatus.CLOSED, result.status());
  }

  @Test
  void rejectsClosingDraftForm() {
    Form form = draftForm(1L);
    when(repository.findById(1L)).thenReturn(Optional.of(form));

    assertThrows(IllegalStateException.class, () -> service.close(1L));
  }

  @Test
  void clonesFormWithFields() {
    Form source = draftForm(1L);
    source.setDescription("Descrição original");
    addField(source, "Reuniões", FormFieldType.NUMBER);
    when(repository.findById(1L)).thenReturn(Optional.of(source));
    saveReturnsArgument();

    CloneFormRequest request = new CloneFormRequest("Relatório 2027", 2027,
        LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 31));
    FormResponse result = service.clone(1L, request);

    assertEquals("Relatório 2027", result.name());
    assertEquals(2027, result.referenceYear());
    assertEquals(FormStatus.DRAFT, result.status());
    assertEquals(1L, result.clonedFromId());
    assertEquals("Descrição original", result.description());
    assertEquals(1, result.fields().size());
    assertEquals("Reuniões", result.fields().get(0).label());
  }

  @Test
  void listsAllForms() {
    Form form = draftForm(1L);
    when(repository.findAllByOrderByReferenceYearDescIdDesc()).thenReturn(List.of(form));
    List<FormResponse> result = service.findAll();
    assertEquals(1, result.size());
    assertEquals("Relatório Anual 2026", result.get(0).name());
  }

  @Test
  void findsFormById() {
    Form form = draftForm(1L);
    when(repository.findById(1L)).thenReturn(Optional.of(form));
    FormResponse result = service.findById(1L);
    assertEquals(1L, result.id());
  }

  @Test
  void throwsWhenFormNotFound() {
    when(repository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> service.findById(99L));
  }

  private CreateFormRequest createRequest() {
    return new CreateFormRequest("Relatório Anual 2026", null, 2026, LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 3, 31), List.of(fieldRequest()));
  }

  private FormFieldRequest fieldRequest() {
    return new FormFieldRequest("Reuniões realizadas", FormFieldType.NUMBER, true, null, null, null,
        null, 1, null);
  }

  private Form draftForm(Long id) {
    Form form = new Form();
    form.setId(id);
    form.setName("Relatório Anual 2026");
    form.setReferenceYear(2026);
    form.setStatus(FormStatus.DRAFT);
    return form;
  }

  private void addField(Form form, String label, FormFieldType type) {
    FormField field = new FormField();
    field.setId((long) (form.getFields().size() + 1));
    field.setForm(form);
    field.setLabel(label);
    field.setType(type);
    field.setRequired(true);
    field.setDisplayOrder(form.getFields().size() + 1);
    field.setOptions(new ArrayList<>());
    form.getFields().add(field);
  }

  private void saveReturnsArgument() {
    when(repository.save(any(Form.class))).thenAnswer(inv -> {
      Form f = inv.getArgument(0);
      if (f.getId() == null) {
        f.setId(1L);
      }
      return f;
    });
  }
}
