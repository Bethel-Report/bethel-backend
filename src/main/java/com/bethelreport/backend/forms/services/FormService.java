package com.bethelreport.backend.forms.services;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FormService {
  private final FormRepository formRepository;
  private final FormMapper formMapper;

  public FormService(FormRepository formRepository, FormMapper formMapper) {
    this.formRepository = formRepository;
    this.formMapper = formMapper;
  }

  public List<FormResponse> findAll() {
    return formMapper.toResponseList(formRepository.findAllByOrderByReferenceYearDescIdDesc());
  }

  public FormResponse findById(Long id) {
    return formMapper.toResponse(findOrThrow(id));
  }

  @Transactional
  public FormResponse create(CreateFormRequest request) {
    validateDates(request.openingDate(), request.closingDate());
    validateFields(request.fields());

    Form form = formMapper.toEntity(request);
    form.setStatus(FormStatus.DRAFT);
    form.setCreatedAt(LocalDateTime.now());
    form.setUpdatedAt(LocalDateTime.now());
    addFields(form, request.fields());

    return formMapper.toResponse(formRepository.save(form));
  }

  @Transactional
  public FormResponse update(Long id, UpdateFormRequest request) {
    Form form = findOrThrow(id);
    requireDraft(form);
    validateDates(request.openingDate(), request.closingDate());
    validateFields(request.fields());

    formMapper.updateEntity(request, form);
    form.setUpdatedAt(LocalDateTime.now());
    form.getFields().clear();
    addFields(form, request.fields());

    return formMapper.toResponse(formRepository.save(form));
  }

  @Transactional
  public FormResponse publish(Long id) {
    Form form = findOrThrow(id);
    requireDraft(form);
    if (form.getFields().isEmpty()) {
      throw new IllegalStateException(
          "Formulário deve possuir ao menos um campo para ser publicado.");
    }
    if (form.getOpeningDate() == null || form.getClosingDate() == null) {
      throw new IllegalStateException(
          "Formulário deve possuir datas de abertura e encerramento para ser publicado.");
    }

    form.setStatus(FormStatus.PUBLISHED);
    form.setUpdatedAt(LocalDateTime.now());
    return formMapper.toResponse(formRepository.save(form));
  }

  @Transactional
  public FormResponse close(Long id) {
    Form form = findOrThrow(id);
    if (form.getStatus() != FormStatus.PUBLISHED) {
      throw new IllegalStateException("Somente formulários publicados podem ser encerrados.");
    }

    form.setStatus(FormStatus.CLOSED);
    form.setUpdatedAt(LocalDateTime.now());
    return formMapper.toResponse(formRepository.save(form));
  }

  @Transactional
  public FormResponse clone(Long id, CloneFormRequest request) {
    Form source = findOrThrow(id);
    validateDates(request.openingDate(), request.closingDate());

    Form clone = new Form();
    clone.setName(request.name());
    clone.setDescription(source.getDescription());
    clone.setReferenceYear(request.referenceYear());
    clone.setStatus(FormStatus.DRAFT);
    clone.setOpeningDate(request.openingDate());
    clone.setClosingDate(request.closingDate());
    clone.setClonedFromId(source.getId());
    clone.setCreatedAt(LocalDateTime.now());
    clone.setUpdatedAt(LocalDateTime.now());

    for (FormField sourceField : source.getFields()) {
      FormField field = new FormField();
      field.setForm(clone);
      field.setLabel(sourceField.getLabel());
      field.setType(sourceField.getType());
      field.setRequired(sourceField.isRequired());
      field.setDisplayOrder(sourceField.getDisplayOrder());
      if (sourceField.getOptions() != null && !sourceField.getOptions().isEmpty()) {
        field.setOptions(new ArrayList<>(sourceField.getOptions()));
      }
      clone.getFields().add(field);
    }

    return formMapper.toResponse(formRepository.save(clone));
  }

  private Form findOrThrow(Long id) {
    return formRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Formulário não encontrado."));
  }

  private void requireDraft(Form form) {
    if (form.getStatus() != FormStatus.DRAFT) {
      throw new IllegalStateException(
          "Formulário publicado ou encerrado não pode ser alterado.");
    }
  }

  private void validateDates(LocalDate opening, LocalDate closing) {
    if (opening != null && closing != null && !closing.isAfter(opening)) {
      throw new IllegalArgumentException(
          "A data de encerramento deve ser posterior à data de abertura.");
    }
  }

  private void validateFields(List<FormFieldRequest> fields) {
    for (FormFieldRequest field : fields) {
      if (field.type() == FormFieldType.SELECT
          && (field.options() == null || field.options().isEmpty())) {
        throw new IllegalArgumentException(
            "Campo do tipo SELECT deve possuir ao menos uma opção.");
      }
    }
  }

  private void addFields(Form form, List<FormFieldRequest> fieldRequests) {
    for (FormFieldRequest request : fieldRequests) {
      FormField field = new FormField();
      field.setForm(form);
      field.setLabel(request.label());
      field.setType(request.type());
      field.setRequired(request.required());
      field.setDisplayOrder(request.displayOrder());
      if (request.options() != null) {
        field.setOptions(new ArrayList<>(request.options()));
      }
      form.getFields().add(field);
    }
  }
}
