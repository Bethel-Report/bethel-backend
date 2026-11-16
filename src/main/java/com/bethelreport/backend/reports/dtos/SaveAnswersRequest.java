package com.bethelreport.backend.reports.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveAnswersRequest(@NotNull @Size(min = 1) List<@Valid ReportAnswerRequest> answers) {
}
