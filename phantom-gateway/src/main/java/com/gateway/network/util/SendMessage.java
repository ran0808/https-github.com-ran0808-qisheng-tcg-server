package com.gateway.network.util;

import com.game.common.protocol.GameProtocol;
import com.game.common.security.JwtTokenProvider;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 消息发送工具类
 * 支持基于Token的通信，自动从Token中解析玩家ID
 */
@Component
@Slf4j
public class SendMessage {
    public static final UUID INVALID_PLAYER_ID = new UUID(0, 0);
    private static JwtTokenProvider jwtTokenProvider;
    private static final Map<Channel, String> channelTokenMap = new HashMap<>();

    @Autowired
    @Lazy
    public void setJwtTokenProvider(JwtTokenProvider jwtTokenProvider) {
        SendMessage.jwtTokenProvider = jwtTokenProvider;
    }

    public static void registerChannelToken(Channel channel, String token) {
        channelTokenMap.put(channel, token);
        log.debug("注册频道Token关联: {} -> {}", channel.id(), token);
    }
    public static void unregisterChannelToken(Channel channel) {
        channelTokenMap.remove(channel);
        log.debug("移除频道Token关联: {}", channel.id());
    }

    public static String getTokenByChannel(Channel channel) {
        return channelTokenMap.get(channel);
    }

    public static void sendMessage(Channel channel, String content, short opcode) {
        if (channel == null || !channel.isActive()) {
            log.warn("尝试向无效频道发送消息");
            return;
        }
        String token = getTokenByChannel(channel);
        UUID playerId = INVALID_PLAYER_ID; // 默认使用无效ID

        // 尝试从Token中解析玩家ID
        if (token != null && !token.isEmpty() && jwtTokenProvider != null) {
            try {
                playerId = UUID.fromString(extractPlayerIdFromToken(token));
            } catch (Exception e) {
                log.warn("从Token解析玩家ID失败: {}", e.getMessage());
            }
        }
        sendMessage(channel, content, opcode, token, playerId);
    }
    public static void sendMessage(Channel channel,String content,short opcode,String token){
        if (channel == null || !channel.isActive()) {
            log.warn("尝试向无效频道发送消息");
            return;
        }
        UUID playerId = INVALID_PLAYER_ID;
        if (token != null && !token.isEmpty() && jwtTokenProvider != null) {
            try {
                playerId = UUID.fromString(extractPlayerIdFromToken(token));
            } catch (Exception e) {
                log.warn("从Token解析玩家ID失败: {}", e.getMessage());
            }
        }
        log.info(String.valueOf(playerId));
        sendMessage(channel, content, opcode, token, playerId);
    }
    /**
     * 发送消息（指定Token和玩家ID）
     */
    public static void sendMessage(Channel channel, String content, short opcode, String token, UUID playerId) {
        if (channel == null || !channel.isActive()) {
            log.warn("尝试向无效频道发送消息");
            return;
        }
        // 如果playerId无效，使用后备方案
        if (playerId == null || INVALID_PLAYER_ID.equals(playerId)) {
            playerId = generateFallbackPlayerId(channel);
        }

        GameProtocol protocol = new GameProtocol();
        protocol.setOpcode(opcode);
        protocol.setPlayerId(playerId);
        if (token != null && !token.isEmpty()) {
            protocol.setToken(token);
        }
        // 设置消息内容
        if (content != null) {
            protocol.setBody(content.getBytes(StandardCharsets.UTF_8));
        }
        // 发送消息
        channel.writeAndFlush(protocol);
        log.info("已向频道 {} 发送消息, 操作码: {}, 玩家ID: {}",
                channel.id(), opcode, playerId);
    }
    private static UUID generateFallbackPlayerId(Channel channel) {
        // 使用频道ID生成UUID（确保唯一性）
        UUID fallbackId = UUID.nameUUIDFromBytes(
                ("fallback-" + channel.id().asLongText()).getBytes()
        );
        log.warn("使用后备玩家ID: {} (频道: {})", fallbackId, channel.id());
        return fallbackId;
    }
    private static String extractPlayerIdFromToken(String token) {
        if (jwtTokenProvider == null) {
            throw new IllegalStateException("JwtTokenProvider未初始化");
        }
        try {
            var claims = jwtTokenProvider.parseToken(token);
            return claims.getId();
        } catch (Exception e) {
            throw new RuntimeException("解析Token失败: " + e.getMessage(), e);
        }
    }

}