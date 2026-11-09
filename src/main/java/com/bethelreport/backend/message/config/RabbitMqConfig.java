package com.bethelreport.backend.message.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Client-side RabbitMQ settings. Broker topology is provisioned by message-broker. */
@Configuration
public class RabbitMqConfig {

  private static final Logger LOG = LoggerFactory.getLogger(RabbitMqConfig.class);

  @Bean
  MessageConverter jacksonMessageConverter() {
    return new JacksonJsonMessageConverter();
  }

  @Bean
  RabbitTemplateCustomizer rabbitTemplateCustomizer() {
    return template -> {
      template.setObservationEnabled(true);
      template.setReturnsCallback(returned -> LOG.warn(
          "RabbitMQ returned message: exchange={}, routingKey={}, replyCode={}, replyText={}",
          returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(),
          returned.getReplyText()));
    };
  }
}
