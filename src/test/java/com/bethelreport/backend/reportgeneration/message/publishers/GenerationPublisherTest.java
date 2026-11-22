package com.bethelreport.backend.reportgeneration.message.publishers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bethelreport.backend.reportgeneration.entities.GenerationType;
import com.bethelreport.backend.reportgeneration.message.dtos.GenerationMessage;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class GenerationPublisherTest {

  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final GenerationPublisher generationPublisher = new GenerationPublisher(rabbitTemplate,
      "report.exchange", "report.generate");

  @Test
  void publishesGenerationMessageAndWaitsForBrokerConfirm() {
    GenerationMessage message = new GenerationMessage(1L, 10L, GenerationType.FINANCEIRO,
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
    doAnswer(invocation -> {
      CorrelationData correlation = invocation.getArgument(3);
      correlation.getFuture().complete(new CorrelationData.Confirm(true, ""));
      return null;
    }).when(rabbitTemplate).convertAndSend(any(String.class), any(String.class),
        any(GenerationMessage.class), any(CorrelationData.class));

    generationPublisher.publish(message);

    verify(rabbitTemplate).convertAndSend(eq("report.exchange"), eq("report.generate"), eq(message),
        any(CorrelationData.class));
  }
}
