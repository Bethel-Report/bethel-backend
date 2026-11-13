package com.bethelreport.backend.message.publishers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bethelreport.backend.message.dtos.EmailMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class EmailPublisherTest {

  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final EmailPublisher emailPublisher = new EmailPublisher(rabbitTemplate,
      "email.exchange", "email.invitation");

  @Test
  void publishesMessageToInvitationRouteAndWaitsForBrokerConfirm() {
    EmailMessage message = new EmailMessage("invited@example.com", "Invitation", "text", "<p>invite</p>");
    doAnswer(invocation -> {
      CorrelationData correlation = invocation.getArgument(3);
      correlation.getFuture().complete(new CorrelationData.Confirm(true, ""));
      return null;
    }).when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(EmailMessage.class),
        any(CorrelationData.class));

    emailPublisher.publish(message);

    verify(rabbitTemplate).convertAndSend(eq("email.exchange"), eq("email.invitation"), eq(message),
        any(CorrelationData.class));
  }
}
