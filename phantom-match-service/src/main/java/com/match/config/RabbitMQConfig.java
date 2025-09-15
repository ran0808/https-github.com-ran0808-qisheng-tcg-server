package com.match.config;

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

import java.util.HashMap;
import java.util.Map;


@Configuration
public class RabbitMQConfig {

    public static final String MATCH_EXCHANGE = "match.exchange";
    public static final String MATCH_QUEUE = "match.queue";
    public static final String MATCH_ROUTING_KEY = "match.routingKey";
    public static final String MATCH_DLQ = "match.dlq";
    public static final String MATCH_DLX = "match.dlx";
    public static final String MATCH_SUCCESS_EXCHANGE = "match.success.exchange";
    public static final String MATCH_SUCCESS_ROUTING_KEY = "match.success.routingKey";
    public static final String MATCH_REMOVE_EXCHANGE = "match.remove.exchange";
    public static final String MATCH_REMOVE_QUEUE = "match.remove.queue";
    public static final String MATCH_REMOVE_ROUTING_KEY = "match.remove.routingKey";
    // 定义直连交换机
    @Bean
    public DirectExchange matchExchange() {
        return new DirectExchange(MATCH_EXCHANGE);
    }
    // 定义死信交换机
    @Bean
    public DirectExchange matchDlx() {
        return new DirectExchange(MATCH_DLX);
    }
    // 绑定队列到交换机
    @Bean
    public Binding matchBinding(Queue matchQueue, DirectExchange matchExchange) {
        return BindingBuilder.bind(matchQueue).to(matchExchange).with(MATCH_ROUTING_KEY);
    }
    // 绑定死信队列到死信交换机
    @Bean
    public Binding matchDlqBinding(Queue matchDlq, DirectExchange matchDlx) {
        return BindingBuilder.bind(matchDlq).to(matchDlx).with(MATCH_DLQ);
    }
    // 配置JSON消息转换器
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    @Bean
    public Queue matchQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MATCH_DLX);
        args.put("x-dead-letter-routing-key", MATCH_DLQ);
        return new Queue(MATCH_QUEUE, true, false, false, args);
    }
    @Bean
    public Queue matchDlq() {
        return new Queue(MATCH_DLQ, true);
    }
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
    @Bean
    public DirectExchange matchRemoveExchange() {
        return new DirectExchange(MATCH_REMOVE_EXCHANGE);
    }

    @Bean
    public Queue matchRemoveQueue() {
        return new Queue(MATCH_REMOVE_QUEUE, true);
    }

    @Bean
    public Binding matchRemoveBinding() {
        return BindingBuilder.bind(matchRemoveQueue())
                .to(matchRemoveExchange())
                .with(MATCH_REMOVE_ROUTING_KEY);
    }
}