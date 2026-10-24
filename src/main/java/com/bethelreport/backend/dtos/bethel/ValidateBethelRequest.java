
package com.bethelreport.backend.dtos.bethel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ValidateBethelRequest(

    @NotNull(message = "Número obrigatório.") @Positive(message = "Inválido.") Integer number,

    @NotBlank(message = "Nome obrigatório.") String name,

    @NotNull(message = "Obrigatória") @PastOrPresent(message = "Inválida") LocalDate foundationDate,

    @NotBlank(message = "Cidade obrigatória.") String city,

    @NotBlank(message = "Endereço obrigatório.") String address,

    @NotBlank(message = "E-mail obrigatório.") @Email(message = "E-mail inválido.") String email,

    @NotBlank(message = "Loja patrocinadora obrigatória.") String sponsoringLodge

) {
}
