package com.gateway.network.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.common.protocol.Opcode;
import com.gateway.config.RabbitMQConfig;
import com.gateway.dto.MatchSuccessMessage;
import com.gateway.dto.PlayerStatus;
import com.gateway.network.util.SendMessage;
import com.gateway.network.util.UpdateStatus;
import com.gateway.service.SessionManager;
import io.netty.channel.Channel;
import org.checkerframework.checker.units.qual.A;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class MatchSuccessConsumer {
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UpdateStatus updateStatus;
    @RabbitListener(queues = RabbitMQConfig.MATCH_SUCCESS_QUEUE)
    public void handleMatchSuccess(MatchSuccessMessage message) {
        try {
            log.info(message.toString());
            if (message.getPlayerId1() == null || message.getPlayerId2() == null || message.getSessionId() == null) {
                log.error("收到无效的匹配成功消息: {}", message);
                return;
            }
            //1.获取玩家的连接通道
            Channel player1Channel = sessionManager.getChannelByToken(sessionManager.getTokenByPlayerIdFromRedis(message.getPlayerId1()));
            Channel player2Channel = sessionManager.getChannelByToken(sessionManager.getTokenByPlayerIdFromRedis(message.getPlayerId2()));
            //2.构建匹配成功消息体
            String successMessage = String.format("匹配成功! 对手: %s, 会话ID: %s",
                    message.getPlayerId1().equals(sessionManager.getPlayerIdByChannel(player1Channel)) ?
                            message.getPlayerId2() : message.getPlayerId1(),
                    message.getSessionId());

            //3.发送匹配成功消息给玩家1
            if (player1Channel != null && player1Channel.isActive()) {
                SendMessage.sendMessage(player1Channel, successMessage, Opcode.MATCH_SUCCESS_OPCODE);
                updateStatus.updatePlayerStatus(message.getPlayerId1(),sessionManager.getTokenByChannel(player1Channel),PlayerStatus.IN_MATCH);
            } else {
                log.warn("玩家 {} 的通道不可用，无法发送匹配成功消息", message.getPlayerId1());
            }
            //4.发送匹配成功消息给玩家2
            if (player2Channel != null && player2Channel.isActive()) {
                SendMessage.sendMessage(player2Channel, successMessage, Opcode.MATCH_SUCCESS_OPCODE);
                updateStatus.updatePlayerStatus(message.getPlayerId2(),sessionManager.getTokenByChannel(player2Channel),PlayerStatus.IN_MATCH);
            }
            else {
                log.warn("玩家 {} 的通道不可用，无法发送匹配成功消息", message.getPlayerId2());
            }
            log.info("已向玩家 {} 和 {} 发送匹配成功通知，会话ID: {}",
                    message.getPlayerId1(), message.getPlayerId2(), message.getSessionId());

        } catch (Exception e) {
            log.error("处理匹配成功消息失败: {}", e.getMessage(), e);
        }
    }
}