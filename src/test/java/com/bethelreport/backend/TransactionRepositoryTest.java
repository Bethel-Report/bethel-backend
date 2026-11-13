package com.bethelreport.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.bethel.repositories.BethelRepository;
import com.bethelreport.backend.transactions.entities.FinancialTransaction;
import com.bethelreport.backend.transactions.entities.TransactionType;
import com.bethelreport.backend.transactions.repositories.TransactionFilters;
import com.bethelreport.backend.transactions.repositories.TransactionRepository;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.users.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TransactionRepositoryTest.Database.class)
@Transactional
class TransactionRepositoryTest {
  @TestConfiguration(proxyBeanMethods = false)
  static class Database {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
      return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }
  }

  @Autowired
  private TransactionRepository repository;
  @Autowired
  private BethelRepository bethels;
  @Autowired
  private UserRepository users;
  @Autowired
  private EntityManager entityManager;

  @Test
  void persistsFiltersUpdatesAndDeletesWithBethelIsolation() {
    Bethel first = bethel(8101);
    Bethel second = bethel(8102);
    User author = new User();
    author.setName("Autor financeiro");
    author.setEmail("transactions@test.com");
    author.setBirthday(LocalDate.of(2000, 1, 1));
    author.setAddress("Rua teste");
    author.setCity("Fortaleza");
    users.saveAndFlush(author);
    LocalDate date = LocalDate.of(2026, 11, 1);
    FinancialTransaction income = transaction(first, author, TransactionType.RECEITA, date);
    FinancialTransaction expense = transaction(first, author, TransactionType.DESPESA, date);
    transaction(first, author, TransactionType.RECEITA, date.minusDays(1));
    transaction(first, author, TransactionType.RECEITA, date.plusDays(1));
    transaction(second, author, TransactionType.RECEITA, date);
    entityManager.flush();
    entityManager.clear();

    assertEquals(4,
        repository.findAll(TransactionFilters.by(first.getId(), null, null, null)).size());
    assertEquals(List.of(income.getId()),
        repository
            .findAll(TransactionFilters.by(first.getId(), TransactionType.RECEITA, date, date))
            .stream().map(FinancialTransaction::getId).toList());
    assertEquals(3, repository
        .findAll(TransactionFilters.by(first.getId(), TransactionType.RECEITA, null, null)).size());
    assertEquals(3,
        repository.findAll(TransactionFilters.by(first.getId(), null, date, null)).size());
    assertEquals(3,
        repository.findAll(TransactionFilters.by(first.getId(), null, null, date)).size());
    assertEquals(4,
        repository
            .findAll(TransactionFilters.by(first.getId(), null, null, null), PageRequest.of(0, 1))
            .getTotalElements());
    assertTrue(repository.findByIdAndBethelId(income.getId(), second.getId()).isEmpty());

    FinancialTransaction persisted = repository.findByIdAndBethelId(income.getId(), first.getId())
        .orElseThrow();
    assertEquals(new BigDecimal("10.10"), persisted.getAmount());
    persisted.setAmount(new BigDecimal("20.25"));
    repository.saveAndFlush(persisted);
    entityManager.clear();
    assertEquals(new BigDecimal("20.25"),
        repository.findById(income.getId()).orElseThrow().getAmount());
    repository.deleteById(expense.getId());
    repository.flush();
    assertFalse(repository.existsById(expense.getId()));
  }

  private Bethel bethel(int number) {
    Bethel entity = new Bethel();
    entity.setNumber(number);
    entity.setName("Bethel " + number);
    entity.setEmail("bethel" + number + "@test.com");
    entity.setAddress("Rua teste");
    entity.setCity("Fortaleza");
    entity.setSponsoringLodge("Loja teste");
    return bethels.saveAndFlush(entity);
  }

  private FinancialTransaction transaction(Bethel bethel, User author, TransactionType type,
      LocalDate date) {
    FinancialTransaction entity = new FinancialTransaction();
    entity.setBethel(bethel);
    entity.setAuthor(author);
    entity.setType(type);
    entity.setAmount(new BigDecimal("10.10"));
    entity.setTransactionDate(date);
    entity.setDescription("Movimentação financeira");
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(entity.getCreatedAt());
    return repository.save(entity);
  }
}
