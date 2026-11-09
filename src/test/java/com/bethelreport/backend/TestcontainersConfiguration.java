package com.bethelreport.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
  }

  @Bean
  @ServiceConnection
  RabbitMQContainer rabbitContainer() {
    return new RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"));
  }

  @Bean
  DirectExchange emailExchange() {
    return new DirectExchange("email.exchange");
  }

  @Bean
  Queue emailQueue() {
    return QueueBuilder.durable("email.queue").deadLetterExchange("dlx.exchange")
        .deadLetterRoutingKey("email.dlq").build();
  }

  @Bean
  Binding emailBinding(Queue emailQueue, DirectExchange emailExchange) {
    return BindingBuilder.bind(emailQueue).to(emailExchange).with("email.send");
  }

  @Bean
  DirectExchange deadLetterExchange() {
    return new DirectExchange("dlx.exchange");
  }

  @Bean
  Queue emailDeadLetterQueue() {
    return QueueBuilder.durable("email.dlq").build();
  }

  @Bean
  Binding emailDeadLetterBinding(Queue emailDeadLetterQueue, DirectExchange deadLetterExchange) {
    return BindingBuilder.bind(emailDeadLetterQueue).to(deadLetterExchange).with("email.dlq");
  }

}
