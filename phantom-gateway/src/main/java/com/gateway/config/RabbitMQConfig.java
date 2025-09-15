package com.gateway.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String MATCH_EXCHANGE = "match.exchange";
    public static final String MATCH_ROUTING_KEY = "match.routingKey";
    public static final String MATCH_SUCCESS_EXCHANGE = "match.success.exchange";
    public static final String MATCH_SUCCESS_ROUTING_KEY = "match.success.routingKey";
    public static final String MATCH_SUCCESS_QUEUE = "match.success.queue";
    public static final String MATCH_REMOVE_EXCHANGE = "match.remove.exchange";
    public static final String MATCH_REMOVE_ROUTING_KEY = "match.remove.routingKey";
    // 定义队列
    @Bean
    public Queue matchSuccessQueue() {
        return new Queue(MATCH_SUCCESS_QUEUE, true); // true 表示持久化队列
    }

    // 定义交换机
    @Bean
    public DirectExchange matchExchange() {
        return new DirectExchange(MATCH_SUCCESS_EXCHANGE, true, false); // 持久化，不自动删除
    }

    // 绑定队列到交换机
    @Bean
    public Binding matchBinding(Queue matchSuccessQueue, DirectExchange matchExchange) {
        return BindingBuilder.bind(matchSuccessQueue)
                .to(matchExchange)
                .with(MATCH_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}