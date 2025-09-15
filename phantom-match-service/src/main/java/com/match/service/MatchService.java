package com.match.service;

import com.game.common.service.PlayerStateService;
import com.game.common.security.JwtTokenProvider;
import com.match.config.RabbitMQConfig;
import com.match.dto.MatchRemoveMessage;
import com.match.dto.MatchSuccessMessage;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Iterator;
import java.util.Set;

@Service
@Slf4j
public class MatchService {
    private static final String MATCH_POOL_KEY = "match:pool";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private PlayerStateService playerStateService;

    public void processMatchRequest(String token) {
        //1.解析token,获得玩家ID
        Claims claims = jwtTokenProvider.parseToken(token);
        String id = claims.getId();
        //2.检查玩家是否在匹配或游戏中
        if (playerStateService.isGaming(id)){
            log.warn("玩家{}已在游戏中，不能加入匹配",id);
            return;
        }
        if (playerStateService.isMatching(id)) {
            log.warn("玩家 {} 已在匹配中", id);
            return;
        }
        //开始匹配
        if (playerStateService.addToMatching(id)){
            addPlayerToMatchPool(id);
            tryFindMatch();
        }
    }
    private void addPlayerToMatchPool(String playerId) {
        redisTemplate.opsForZSet().add(MATCH_POOL_KEY, playerId, System.currentTimeMillis());
        log.info("玩家 {} 已加入匹配池", playerId);
    }
    private void tryFindMatch() {
        Set<Object> players = redisTemplate.opsForZSet().range(MATCH_POOL_KEY, 0, -1);
        if (players != null && players.size() >= 2) {
            // 简单的匹配算法：选择最早加入的两个玩家
            Iterator<Object> iterator = players.iterator();
            String player1 = (String) iterator.next();
            String player2 = (String) iterator.next();
            // 从匹配池中移除
            redisTemplate.opsForZSet().remove(MATCH_POOL_KEY, player1, player2);
            // 创建游戏会话
            String sessionId = gameSessionService.createGameSession(player1,player2);
            log.info("匹配成功: 玩家 {} 和 玩家 {}, 会话ID: {}", player1, player2, sessionId);
            // 通知玩家匹配成功
            notifyPlayersMatchSuccess(player1, player2, sessionId);
        }
    }
    private void notifyPlayersMatchSuccess(String player1, String player2, String sessionId) {
        try {
            //将玩家状态从匹配改为游戏状态
            playerStateService.removeFromMatching(player1);
            playerStateService.removeFromMatching(player2);
            playerStateService.addToGaming(player1);
            playerStateService.addToGaming(player2);
            // 创建匹配成功消息
            MatchSuccessMessage message = new MatchSuccessMessage(player1,player2,sessionId);
            // 发送消息到通知队列
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MATCH_SUCCESS_EXCHANGE,
                    RabbitMQConfig.MATCH_SUCCESS_ROUTING_KEY,
                    message
            );
            log.info(message.toString());
            log.info("已发送匹配成功通知: 会话ID {}", sessionId);
        } catch (Exception e) {
            log.error("发送匹配成功通知失败: {}", e.getMessage());
        }
    }
    /**
     * 从匹配池中移除玩家
     */
    public void removePlayerFromMatchPool(String playerId) {
        try {
            // 从Redis匹配池中移除玩家
            redisTemplate.opsForZSet().remove(MATCH_POOL_KEY, playerId);
            // 从内存状态中移除玩家
            playerStateService.removeFromMatching(playerId);
            log.info("玩家 {} 已从匹配池中移除", playerId);
        } catch (Exception e) {
            log.error("从匹配池移除玩家失败: {}", e.getMessage());
        }
    }
}
