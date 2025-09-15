package com.match;

import com.match.config.RabbitMQConfig;
import com.match.dto.MatchRemoveMessage;
import com.match.dto.MatchRequestMessage;
import com.match.service.MatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MatchQueueConsumer {

    @Autowired
    private MatchService matchService;

    @RabbitListener(queues = RabbitMQConfig.MATCH_QUEUE)
    public void handleMatchRequest(MatchRequestMessage message) {
        try {
            log.info("收到匹配请求: {}", message);

            // 处理匹配逻辑
            matchService.processMatchRequest(message.getToken());

        } catch (Exception e) {
            log.error("处理匹配请求失败: {}", e.getMessage());
            // 可以根据业务需求决定是否重新排队
            throw new AmqpRejectAndDontRequeueException("处理匹配请求失败", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.MATCH_DLQ)
    public void handleFailedMatchRequest(MatchRequestMessage failedMessage) {
        log.warn("收到死信队列消息: {}", failedMessage);
    }

    /**
     * 处理移除匹配请求
     */
    @RabbitListener(queues = RabbitMQConfig.MATCH_REMOVE_QUEUE)
    public void processRemoveMatchRequest(MatchRemoveMessage message) {
        String playerId = message.getPlayerId();
        matchService.removePlayerFromMatchPool(playerId);
        log.info("已从匹配池中移除玩家 {}", playerId);
    }


}