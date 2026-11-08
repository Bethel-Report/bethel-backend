package com.bethelreport.backend.message.publishers;

import com.bethelreport.backend.message.config.RabbitMqConfig;
import com.bethelreport.backend.message.dtos.EmailMessage;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailPublisher {

  private static final long CONFIRM_TIMEOUT_SECONDS = 5;

  private final RabbitTemplate rabbitTemplate;

  public void publish(EmailMessage message) {
    CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
    rabbitTemplate.convertAndSend(RabbitMqConfig.EMAIL_EXCHANGE, RabbitMqConfig.EMAIL_ROUTING_KEY,
        message, correlation);
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
  }
}
