
package com.bethelreport.backend.dtos.bethel;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

public record UpdateBethelRequest(

    @NotNull(message = "O número do Bethel é obrigatório.")
    @Positive(message = "O número do Bethel deve ser maior que zero.")
    Integer number,

    @NotBlank(message = "O nome do Bethel é obrigatório.")
    String name,

    @NotNull(message = "A data de fundação é obrigatória.")
    @PastOrPresent(message = "A data de fundação não pode ser futura.")
    LocalDate foundationDate,

    @NotBlank(message = "A cidade é obrigatória.")
    String city,

    @NotBlank(message = "O endereço é obrigatório.")
    String address,

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um endereço de e-mail válido.")
    String email,

    @NotBlank(message = "A Loja Maçônica patrocinadora é obrigatória.")
    String sponsoringLodge

) {}
