package com.bethelreport.backend.message.publishers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bethelreport.backend.message.dtos.EmailMessage;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class EmailPublisherTest {

  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final EmailPublisher emailPublisher = new EmailPublisher(rabbitTemplate);

  @Test
  void publishesMessageToInvitationRoute() {
    EmailMessage message = new EmailMessage("invited@example.com", "Invitation", "invitation",
        Map.of("invitationLink", "https://example.com/invite"));

    emailPublisher.publish(message);

    verify(rabbitTemplate).convertAndSend("email.exchange", "email.invitation", message);
  }
}
