package com.match.component;

import com.match.config.RabbitMQConfig;
import com.rabbitmq.client.AMQP;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class MatchQueueMonitor {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 30000) // 每30秒检查一次
    public void monitorQueue() {
        try {
            Integer messageCount = rabbitTemplate.execute(channel -> {
                AMQP.Queue.DeclareOk declareOk = channel.queueDeclarePassive(RabbitMQConfig.MATCH_QUEUE);
                return declareOk.getMessageCount();
            });

            log.info("匹配队列当前消息数量: {}", messageCount);

            if (messageCount > 1000) {
                log.warn("匹配队列积压严重，当前消息数: {}", messageCount);
            }
        } catch (Exception e) {
            log.error("监控匹配队列失败: {}", e.getMessage());
        }
    }
}
