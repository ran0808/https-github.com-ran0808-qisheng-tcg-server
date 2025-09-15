package com.gateway.service;

import com.gateway.config.RabbitMQConfig;
import com.gateway.dto.MatchRemoveMessage;
import com.gateway.dto.MatchRequestMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class MatchService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    public void addToMatchQueue(String playerId,String token) {
        try {
            // 创建匹配请求消息
            MatchRequestMessage message = new MatchRequestMessage(
                    token,
                    System.currentTimeMillis(),
                    playerId
            );
            // 发送消息到RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MATCH_EXCHANGE,
                    RabbitMQConfig.MATCH_ROUTING_KEY,
                    message,
                    m -> {
                        // 设置消息持久化
                        m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return m;
                    }
            );
            log.info("已发送玩家 {} 的匹配请求到消息队列", playerId);
        } catch (Exception e) {
            log.error("发送匹配请求到消息队列失败: {}", e.getMessage());
        }
    }
    public void removeFromMatchQueue(String playerId) {
        try {
            MatchRemoveMessage message = new MatchRemoveMessage(playerId);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MATCH_REMOVE_EXCHANGE,
                    RabbitMQConfig.MATCH_REMOVE_ROUTING_KEY,
                    message
            );
            log.info("已发送移除玩家 {} 的匹配请求", playerId);
        } catch (Exception e) {
            log.error("发送移除匹配请求失败: {}", e.getMessage());
        }
    }
}
