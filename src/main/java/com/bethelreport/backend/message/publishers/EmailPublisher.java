package com.bethelreport.backend.message.publishers;

import com.bethelreport.backend.message.dtos.EmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailPublisher {

  private final RabbitTemplate rabbitTemplate;

  public void publish(EmailMessage message) {
    rabbitTemplate.convertAndSend("email.exchange", "email.invitation", message);
  }
}
