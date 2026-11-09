package com.bethelreport.backend.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bethelreport.backend.TestcontainersConfiguration;
import com.bethelreport.backend.message.dtos.EmailMessage;
import com.bethelreport.backend.message.publishers.EmailPublisher;
import com.rabbitmq.client.GetResponse;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RabbitMqIntegrationTest {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Autowired
  private EmailPublisher emailPublisher;

  @Test
  void publishedEmailMessageArrivesInQueueAsJson() {
    EmailMessage email = new EmailMessage("test@example.com", "Test Subject", "plain text",
        "<p>rendered html</p>");

    emailPublisher.publish(email);

    Message raw = rabbitTemplate.receive("email.queue", TimeUnit.SECONDS.toMillis(5));
    assertNotNull(raw, "Message should arrive in email.queue");
    assertEquals("application/json", raw.getMessageProperties().getContentType());
    String body = new String(raw.getBody());
    assertTrue(body.contains("test@example.com"));
    assertTrue(body.contains("Test Subject"));
  }

  @Test
  void rejectedMessageIsRoutedToDeadLetterQueue() {
    rabbitTemplate.convertAndSend("email.exchange", "email.send",
        new EmailMessage("dlq@example.com", "DLQ Test", "plain text", null));

    rabbitTemplate.execute(channel -> {
      GetResponse response = channel.basicGet("email.queue", false);
      assertNotNull(response, "Message should be in email.queue");
      channel.basicReject(response.getEnvelope().getDeliveryTag(), false);
      return null;
    });

    Message dlqMessage = rabbitTemplate.receive("email.dlq", TimeUnit.SECONDS.toMillis(5));
    assertNotNull(dlqMessage, "Rejected message should arrive in email.dlq");
    assertTrue(new String(dlqMessage.getBody()).contains("dlq@example.com"));
  }
}
