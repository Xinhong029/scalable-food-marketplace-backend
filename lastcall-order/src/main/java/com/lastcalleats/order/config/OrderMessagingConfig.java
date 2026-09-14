package com.lastcalleats.order.config;

import static com.lastcalleats.order.messaging.OrderMessagingTopology.DEAD_LETTER_EXCHANGE;
import static com.lastcalleats.order.messaging.OrderMessagingTopology.ORDER_CREATED_DEAD_QUEUE;
import static com.lastcalleats.order.messaging.OrderMessagingTopology.ORDER_CREATED_DEAD_ROUTING_KEY;
import static com.lastcalleats.order.messaging.OrderMessagingTopology.ORDER_CREATED_QUEUE;
import static com.lastcalleats.order.messaging.OrderMessagingTopology.ORDER_CREATED_ROUTING_KEY;
import static com.lastcalleats.order.messaging.OrderMessagingTopology.ORDER_EXCHANGE;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Declares the durable RabbitMQ topology used by order-created events. */
@Configuration
public class OrderMessagingConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(OrderMessagingConfig.class);

  /** Actively declares the exchange, queues, and bindings on application startup. */
  @Bean
  public AmqpAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }

  /** Opens the RabbitMQ connection and declares the configured topology. */
  @Bean
  public ApplicationRunner rabbitTopologyInitializer(AmqpAdmin rabbitAdmin) {
    return args -> {
      try {
        rabbitAdmin.initialize();
      } catch (AmqpException exception) {
        LOGGER.warn("RabbitMQ topology declaration deferred until the broker reconnects: {}",
            exception.getMessage());
      }
    };
  }

  @Bean
  public TopicExchange orderExchange() {
    return new TopicExchange(ORDER_EXCHANGE, true, false);
  }

  @Bean
  public Queue orderCreatedQueue() {
    return QueueBuilder.durable(ORDER_CREATED_QUEUE)
        .deadLetterExchange(DEAD_LETTER_EXCHANGE)
        .deadLetterRoutingKey(ORDER_CREATED_DEAD_ROUTING_KEY)
        .build();
  }

  @Bean
  public Binding orderCreatedBinding(
      @Qualifier("orderCreatedQueue") Queue orderCreatedQueue,
      TopicExchange orderExchange) {
    return BindingBuilder.bind(orderCreatedQueue)
        .to(orderExchange)
        .with(ORDER_CREATED_ROUTING_KEY);
  }

  @Bean
  public DirectExchange orderDeadLetterExchange() {
    return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
  }

  @Bean
  public Queue orderCreatedDeadLetterQueue() {
    return QueueBuilder.durable(ORDER_CREATED_DEAD_QUEUE).build();
  }

  @Bean
  public Binding orderCreatedDeadLetterBinding(
      @Qualifier("orderCreatedDeadLetterQueue") Queue orderCreatedDeadLetterQueue,
      DirectExchange orderDeadLetterExchange) {
    return BindingBuilder.bind(orderCreatedDeadLetterQueue)
        .to(orderDeadLetterExchange)
        .with(ORDER_CREATED_DEAD_ROUTING_KEY);
  }

  @Bean
  public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }
}
