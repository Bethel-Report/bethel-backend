package com.bethelreport.backend.message.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

  private static final Logger LOG = LoggerFactory.getLogger(RabbitMqConfig.class);

  @Bean
  MessageConverter jacksonMessageConverter() {
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
    DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
    typeMapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
    converter.setJavaTypeMapper(typeMapper);
    return converter;
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
