package com.bethelreport.backend.message.publishers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bethelreport.backend.message.dtos.EmailMessage;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class EmailPublisherTest {

  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final EmailPublisher emailPublisher = new EmailPublisher(rabbitTemplate);

  @Test
  void publishesMessageToEmailRoute() {
    simulateConfirm(true);
    EmailMessage message = new EmailMessage("invited@example.com", "Invitation", "invitation",
        Map.of("invitationLink", "https://example.com/invite"));

    assertDoesNotThrow(() -> emailPublisher.publish(message));

    verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class),
        any(CorrelationData.class));
  }

  @Test
  void throwsOnBrokerNack() {
    simulateConfirm(false);
    EmailMessage message = new EmailMessage("invited@example.com", "Invitation", "invitation",
        Map.of("invitationLink", "https://example.com/invite"));

    assertThrows(AmqpException.class, () -> emailPublisher.publish(message));
  }

  private void simulateConfirm(boolean ack) {
    doAnswer(invocation -> {
      CorrelationData cd = invocation.getArgument(3);
      cd.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "nack-reason"));
      return null;
    }).when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class),
        any(CorrelationData.class));
  }
}
