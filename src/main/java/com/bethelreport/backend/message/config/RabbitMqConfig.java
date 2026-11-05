package com.bethelreport.backend.message.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

  private static final Logger log = LoggerFactory.getLogger(RabbitMqConfig.class);

  public static final String EMAIL_EXCHANGE = "email.exchange";
  public static final String EMAIL_QUEUE = "email.queue";
  public static final String EMAIL_ROUTING_KEY = "email.send";

  public static final String DLX_EXCHANGE = "dlx.exchange";
  public static final String EMAIL_DLQ = "email.dlq";

  @Bean
  DirectExchange emailExchange() {
    return new DirectExchange(EMAIL_EXCHANGE);
  }

  @Bean
  Queue emailQueue() {
    return QueueBuilder.durable(EMAIL_QUEUE).deadLetterExchange(DLX_EXCHANGE)
        .deadLetterRoutingKey(EMAIL_DLQ).build();
  }

  @Bean
  Binding emailBinding(Queue emailQueue, DirectExchange emailExchange) {
    return BindingBuilder.bind(emailQueue).to(emailExchange).with(EMAIL_ROUTING_KEY);
  }

  @Bean
  DirectExchange dlxExchange() {
    return new DirectExchange(DLX_EXCHANGE);
  }

  @Bean
  Queue emailDlq() {
    return QueueBuilder.durable(EMAIL_DLQ).build();
  }

  @Bean
  Binding emailDlqBinding(Queue emailDlq, DirectExchange dlxExchange) {
    return BindingBuilder.bind(emailDlq).to(dlxExchange).with(EMAIL_DLQ);
  }

  @Bean
  MessageConverter jacksonMessageConverter() {
    return new JacksonJsonMessageConverter();
  }

  @Bean
  RabbitTemplateCustomizer rabbitTemplateCustomizer() {
    return template -> {
      template.setObservationEnabled(true);
      template.setConfirmCallback((correlationData, ack, cause) -> {
        if (!ack) {
          log.error("Mensagem nao confirmada pelo broker: correlationData={}, causa={}",
              correlationData, cause);
        }
      });
      template.setReturnsCallback(returned -> log.warn(
          "Mensagem retornada: exchange={}, routingKey={}, replyCode={}, replyText={}",
          returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(),
          returned.getReplyText()));
    };
  }
}
