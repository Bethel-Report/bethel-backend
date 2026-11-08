package com.bethelreport.backend.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bethelreport.backend.TestcontainersConfiguration;
import com.bethelreport.backend.message.config.RabbitMqConfig;
import com.bethelreport.backend.message.dtos.EmailMessage;
import com.bethelreport.backend.message.publishers.EmailPublisher;
import com.rabbitmq.client.GetResponse;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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
  private RabbitAdmin rabbitAdmin;

  @Autowired
  private EmailPublisher emailPublisher;

  @Test
  void declaredTopologyIncludesEmailQueueAndDlq() {
    Properties emailQueueProps = rabbitAdmin.getQueueProperties(RabbitMqConfig.EMAIL_QUEUE);
    assertNotNull(emailQueueProps, "email.queue should exist");

    Properties dlqProps = rabbitAdmin.getQueueProperties(RabbitMqConfig.EMAIL_DLQ);
    assertNotNull(dlqProps, "email.dlq should exist");
  }

  @Test
  void publishedEmailMessageArrivesInQueueAsJson() {
    EmailMessage email = new EmailMessage("test@example.com", "Test Subject", "test-template",
        Map.of("key", "value"));

    emailPublisher.publish(email);

    Message raw = rabbitTemplate.receive(RabbitMqConfig.EMAIL_QUEUE, TimeUnit.SECONDS.toMillis(5));
    assertNotNull(raw, "Message should arrive in email.queue");
    assertEquals("application/json", raw.getMessageProperties().getContentType());
    String body = new String(raw.getBody());
    assertTrue(body.contains("test@example.com"));
    assertTrue(body.contains("Test Subject"));
  }

  @Test
  void rejectedMessageIsRoutedToDeadLetterQueue() {
    rabbitTemplate.convertAndSend(RabbitMqConfig.EMAIL_EXCHANGE, RabbitMqConfig.EMAIL_ROUTING_KEY,
        new EmailMessage("dlq@example.com", "DLQ Test", "test-template", Map.of("key", "value")));

    rabbitTemplate.execute(channel -> {
      GetResponse response = channel.basicGet(RabbitMqConfig.EMAIL_QUEUE, false);
      assertNotNull(response, "Message should be in email.queue");
      channel.basicReject(response.getEnvelope().getDeliveryTag(), false);
      return null;
    });

    Message dlqMessage = rabbitTemplate.receive(RabbitMqConfig.EMAIL_DLQ,
        TimeUnit.SECONDS.toMillis(5));
    assertNotNull(dlqMessage, "Rejected message should arrive in email.dlq");
    assertTrue(new String(dlqMessage.getBody()).contains("dlq@example.com"));
  }
}
