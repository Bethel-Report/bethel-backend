package com.bethelreport.backend.reports.message.publishers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bethelreport.backend.reports.entities.ReportType;
import com.bethelreport.backend.reports.message.dtos.ReportMessage;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class ReportPublisherTest {

  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final ReportPublisher reportPublisher = new ReportPublisher(rabbitTemplate,
      "report.exchange", "report.generate");

  @Test
  void publishesReportMessageAndWaitsForBrokerConfirm() {
    ReportMessage message = new ReportMessage(1L, 10L, ReportType.FINANCEIRO,
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
    doAnswer(invocation -> {
      CorrelationData correlation = invocation.getArgument(3);
      correlation.getFuture().complete(new CorrelationData.Confirm(true, ""));
      return null;
    }).when(rabbitTemplate).convertAndSend(any(String.class), any(String.class),
        any(ReportMessage.class), any(CorrelationData.class));

    reportPublisher.publish(message);

    verify(rabbitTemplate).convertAndSend(eq("report.exchange"), eq("report.generate"), eq(message),
        any(CorrelationData.class));
  }
}
