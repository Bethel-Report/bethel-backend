package com.bethelreport.backend.transactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.transactions.dtos.TransactionPage;
import com.bethelreport.backend.transactions.dtos.TransactionRequest;
import com.bethelreport.backend.transactions.entities.FinancialTransaction;
import com.bethelreport.backend.transactions.entities.TransactionType;
import com.bethelreport.backend.transactions.mappers.TransactionMapper;
import com.bethelreport.backend.users.entities.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class TransactionMapperTest {
  private final TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

  @Test
  void updatePreservesIdentityOwnershipAndAuditFields() {
    FinancialTransaction entity = new FinancialTransaction();
    Bethel bethel = new Bethel();
    User author = new User();
    LocalDateTime timestamp = LocalDateTime.of(2026, 11, 1, 12, 0);
    entity.setId(15L);
    entity.setBethel(bethel);
    entity.setAuthor(author);
    entity.setCreatedAt(timestamp);
    entity.setUpdatedAt(timestamp);
    TransactionRequest request = new TransactionRequest(TransactionType.DESPESA,
        new BigDecimal("25.50"), LocalDate.of(2026, 11, 2), " Material ");

    mapper.updateEntity(request, entity);

    assertEquals(15L, entity.getId());
    assertSame(bethel, entity.getBethel());
    assertSame(author, entity.getAuthor());
    assertEquals(timestamp, entity.getCreatedAt());
    assertEquals(timestamp, entity.getUpdatedAt());
    assertEquals(request.type(), entity.getType());
    assertEquals(request.amount(), entity.getAmount());
    assertEquals(request.transactionDate(), entity.getTransactionDate());
    assertEquals("Material", entity.getDescription());
  }

  @Test
  void mapsPageContentAndPreservesPaginationIncludingEmptyPages() {
    FinancialTransaction entity = mapper.toEntity(new TransactionRequest(TransactionType.RECEITA,
        new BigDecimal("50.00"), LocalDate.of(2026, 11, 1), " Receita "));
    Bethel bethel = new Bethel();
    bethel.setId(10L);
    User author = new User();
    author.setId(7L);
    entity.setBethel(bethel);
    entity.setAuthor(author);
    entity.setId(15L);

    TransactionPage result = mapper
        .toPage(new PageImpl<>(List.of(entity), PageRequest.of(1, 2), 7));

    assertEquals(1, result.number());
    assertEquals(2, result.size());
    assertEquals(7, result.totalElements());
    assertEquals(4, result.totalPages());
    assertEquals(15L, result.content().getFirst().id());
    assertEquals(10L, result.content().getFirst().bethelId());
    assertEquals(7L, result.content().getFirst().userId());
    assertEquals("Receita", result.content().getFirst().description());
    TransactionPage empty = mapper.toPage(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    assertTrue(empty.content().isEmpty());
    assertEquals(0, empty.totalElements());
    assertEquals(0, empty.totalPages());
    assertEquals(20, empty.size());
  }
}
