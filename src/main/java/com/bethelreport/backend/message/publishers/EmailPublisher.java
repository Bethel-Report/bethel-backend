package com.bethelreport.backend.message.publishers;

import com.bethelreport.backend.message.dtos.EmailMessage;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailPublisher {

  private static final long CONFIRM_TIMEOUT_SECONDS = 5;

  private final RabbitTemplate rabbitTemplate;
  private final String exchange;
  private final String routingKey;

  public EmailPublisher(RabbitTemplate rabbitTemplate,
      @Value("${app.messaging.email.exchange}") String exchange,
      @Value("${app.messaging.email.routing-key}") String routingKey) {
    this.rabbitTemplate = rabbitTemplate;
    this.exchange = exchange;
    this.routingKey = routingKey;
  }

  public void publish(EmailMessage message) {
    CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
    rabbitTemplate.convertAndSend(exchange, routingKey, message, correlation);
    waitForConfirm(correlation);
  }

  private void waitForConfirm(CorrelationData correlation) {
    CorrelationData.Confirm confirm;
    try {
      confirm = correlation.getFuture().orTimeout(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
    } catch (CompletionException e) {
      throw new AmqpException("Confirm failed: id=" + correlation.getId(), e);
    }
    if (!confirm.ack()) {
      throw new AmqpException(
          "Broker NACK: id=" + correlation.getId() + ", reason=" + confirm.reason());
    }
    if (correlation.getReturned() != null) {
      throw new AmqpException("Broker could not route email: id=" + correlation.getId());
    }
  }
}
