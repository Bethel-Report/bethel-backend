package com.bethelreport.backend.reportgeneration.message.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportRabbitMqConfig {

  @Bean
  DirectExchange reportExchange(
      @Value("${app.messaging.report.exchange}") String exchangeName) {
    return new DirectExchange(exchangeName, true, false);
  }

  @Bean
  Queue reportGenerateQueue(
      @Value("${app.messaging.report.queue}") String queueName) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding reportGenerateBinding(Queue reportGenerateQueue, DirectExchange reportExchange,
      @Value("${app.messaging.report.routing-key}") String routingKey) {
    return BindingBuilder.bind(reportGenerateQueue).to(reportExchange).with(routingKey);
  }

  @Bean
  Queue reportResultQueue(
      @Value("${app.messaging.report.result-queue}") String queueName) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding reportResultBinding(Queue reportResultQueue, DirectExchange reportExchange,
      @Value("${app.messaging.report.result-routing-key}") String routingKey) {
    return BindingBuilder.bind(reportResultQueue).to(reportExchange).with(routingKey);
  }
}
